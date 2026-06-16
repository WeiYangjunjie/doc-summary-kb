package com.example.dockb.client;

import com.example.dockb.client.dto.ChatRequest;
import com.example.dockb.client.dto.ChatResponse;
import com.example.dockb.client.dto.QaResult;
import com.example.dockb.client.dto.RankedHit;
import com.example.dockb.config.M3Properties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * M3Client 的默认实现：基于 Spring RestClient 调用 OpenAI 兼容协议。
 *
 * <p>失败重试 1 次（指数退避）。
 */
@Slf4j
@Component
public class DefaultM3Client implements M3Client {

    private static final Pattern JSON_ARRAY = Pattern.compile("\\[.*?\\]", Pattern.DOTALL);
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*}");
    private static final Pattern QUOTED_STRING = Pattern.compile("\"([^\"]+)\"");

    private final RestClient restClient;
    private final RestClient streamingRestClient;
    private final M3Properties props;
    private final ObjectMapper objectMapper;

    public DefaultM3Client(RestClient m3RestClient,
                           @Qualifier("streamingRestClient") RestClient streamingRestClient,
                           M3Properties props, ObjectMapper objectMapper) {
        this.restClient = m3RestClient;
        this.streamingRestClient = streamingRestClient;
        this.props = props;
        this.objectMapper = objectMapper;
        if (!props.isKeyConfigured()) {
            log.warn("[M3] API key is not configured (still placeholder). M3 calls will fail until you set "
                    + "environment variable MiniMax_API_KEY or change 'MiniMax.api-key' in application.yml. "
                    + "You can still test upload/list endpoints.");
        } else {
            log.info("[M3] Using model={}, baseUrl={}", props.getModel(), props.getBaseUrl());
        }
    }

    @Override
    public boolean ping() {
        try {
            ChatResponse resp = chat(ChatRequest.builder()
                    .model(props.getModel())
                    .messages(List.of(ChatRequest.Message.builder()
                            .role("user")
                            .content("ping")
                            .build()))
                    .build());
            return resp != null && resp.firstContent() != null;
        } catch (Exception e) {
            log.debug("[M3] ping failed: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 分类 ====================

    @Override
    public String classify(String text, List<String> candidates) {
        return classify(text, candidates, null);
    }

    @Override
    public String classify(String text, List<String> candidates, String model) {
        String prompt = "你是文档分类助手。请从候选类别中选一个最合适的，**只返回类别名称本身**，不要其他任何内容。\n"
                + "候选类别：" + String.join("、", candidates) + "\n"
                + "文档内容（前 2000 字）：\n" + truncate(text, 2000);
        String content = callAsString(prompt, model);
        return matchCandidate(content, candidates);
    }

    // ==================== 摘要 ====================

    @Override
    public String summarize(String text) {
        return summarize(text, null);
    }

    @Override
    public String summarize(String text, String model) {
        String prompt = "你是文档摘要助手。请用中文输出 200~500 字的摘要，要求保留核心观点，输出纯文本：\n"
                + truncate(text, 6000);
        return callAsString(prompt, model);
    }

    // ==================== 标签抽取 ====================

    @Override
    public List<String> extractTags(String text) {
        return extractTags(text, null);
    }

    @Override
    public List<String> extractTags(String text, String model) {
        String prompt = "你是关键词提取助手。请从下面的文档中提取 3~8 个关键标签，输出 **严格的 JSON 数组** 形式"
                + "（如 [\"AI\",\"大模型\"]），不要其他任何说明：\n" + truncate(text, 4000);
        String content = callAsString(prompt, model);
        return parseStringList(content);
    }

    // ==================== 重排 ====================

    @Override
    public List<RankedHit> rerank(String query, List<String> candidates) {
        return rerank(query, candidates, null);
    }

    @Override
    public List<RankedHit> rerank(String query, List<String> candidates, String model) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("你是检索重排助手。用户问题：").append(query).append("\n");
        sb.append("下面是一个候选段落列表，请按与问题的相关度从高到低排序并打分（0~1）。");
        sb.append("**严格返回 JSON 数组**，每个元素 {\"index\": <候选原下标 0-based>, \"score\": <float>}：\n");
        for (int i = 0; i < candidates.size(); i++) {
            sb.append("[").append(i).append("] ")
              .append(truncate(candidates.get(i), 400))
              .append("\n");
        }
        ChatResponse resp = callRaw(ChatRequest.builder()
                .model(resolveModel(model))
                .messages(List.of(ChatRequest.Message.builder().role("user").content(sb.toString()).build()))
                .build());
        String content = resp == null ? null : resp.firstContent();
        return parseRerank(content, candidates.size());
    }

    // ==================== 问答 ====================

    @Override
    public QaResult answer(String question, List<String> context) {
        return answer(question, context, null);
    }

    @Override
    public QaResult answer(String question, List<String> context, String model) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是文档问答助手。请仅依据下面提供的【参考资料】回答用户问题。\n");
        sb.append("【参考资料】（每条前是 [i] 编号）：\n");
        if (context != null) {
            for (int i = 0; i < context.size(); i++) {
                sb.append("[").append(i).append("] ").append(truncate(context.get(i), 1500)).append("\n");
            }
        }
        sb.append("\n用户问题：").append(question).append("\n");
        sb.append("要求：\n");
        sb.append("1. 仅返回 **严格的 JSON**，形如 {\"answer\":\"...\",\"citations\":[{\"index\":0,\"snippet\":\"...\"}]}；\n");
        sb.append("2. citations 必须引用上面资料中的编号；answer 用中文。");

        ChatResponse resp = callRaw(ChatRequest.builder()
                .model(resolveModel(model))
                .messages(List.of(ChatRequest.Message.builder().role("user").content(sb.toString()).build()))
                .build());
        String content = resp == null ? null : resp.firstContent();
        return parseQa(content);
    }

    // ==================== 流式问答 ====================

    @Override
    public Flux<String> answerStream(String question, List<String> context) {
        return answerStream(question, context, null);
    }

    @Override
    public Flux<String> answerStream(String question, List<String> context, String model) {
        String effectiveModel = resolveModel(model);
        log.info("[M3] answerStream model={}", effectiveModel);
        // 构建 prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是文档问答助手。请仅依据下面提供的【参考资料】回答用户问题。\n");
        prompt.append("【参考资料】（每条前是 [i] 编号）：\n");
        if (context != null) {
            for (int i = 0; i < context.size(); i++) {
                prompt.append("[").append(i).append("] ").append(truncate(context.get(i), 1500)).append("\n");
            }
        }
        prompt.append("\n用户问题：").append(question).append("\n");
        prompt.append("要求：\n");
        prompt.append("1. 仅返回 **严格的 JSON**，形如 {\"answer\":\"...\",\"citations\":[{\"index\":0,\"snippet\":\"...\"}]}；\n");
        prompt.append("2. citations 必须引用上面资料中的编号；answer 用中文。");

        ChatRequest request = ChatRequest.builder()
                .model(effectiveModel)
                .stream(true)
                .messages(List.of(ChatRequest.Message.builder()
                        .role("user")
                        .content(prompt.toString())
                        .build()))
                .build();

        return Flux.create(emitter -> {
            try {
                doStreamAnswer(request, emitter);
            } catch (Exception e) {
                log.error("[M3] answerStream error: {}", e.getMessage());
                emitter.error(new M3Exception("M3 streaming failed: " + e.getMessage(), e));
            }
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    // ==================== 通用 ====================

    @Override
    public ChatResponse chat(ChatRequest request) {
        return callRaw(request);
    }

    // ----------------------------- 内部工具 -----------------------------

    private String resolveModel(String model) {
        return (model != null && !model.isBlank()) ? model : props.getModel();
    }

    private String callAsString(String userPrompt) {
        return callAsString(userPrompt, null);
    }

    private String callAsString(String userPrompt, String model) {
        ChatResponse resp = callRaw(ChatRequest.builder()
                .model(resolveModel(model))
                .messages(List.of(ChatRequest.Message.builder().role("user").content(userPrompt).build()))
                .build());
        String content = resp == null ? null : resp.firstContent();
        return content == null ? "" : content.trim();
    }

    private ChatResponse callRaw(ChatRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            request.setModel(props.getModel());
        }
        ChatResponse last = null;
        int retries = Math.max(0, props.getMaxRetries());
        for (int attempt = 0; attempt <= retries; attempt++) {
            try {
                return doCall(request);
            } catch (Exception e) {
                last = null;
                log.warn("[M3] call failed (attempt {}/{}): {}", attempt + 1, retries + 1, e.getMessage());
                if (attempt < retries) {
                    sleepBackoff(attempt);
                } else {
                    throw new M3Exception("M3 call failed: " + e.getMessage(), e);
                }
            }
        }
        throw new M3Exception("M3 call failed after retries");
    }

    private ChatResponse doCall(ChatRequest request) {
        String url = trimTrailingSlash(props.getBaseUrl()) + "/chat/completions";
        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.resolveApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatResponse.class);
    }

    private void sleepBackoff(int attempt) {
        try {
            long ms = (long) (500L * Math.pow(2, attempt));
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String trimTrailingSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String matchCandidate(String content, List<String> candidates) {
        if (content == null || content.isBlank()) {
            return candidates.get(0);
        }
        String trimmed = content.trim();
        for (String c : candidates) {
            if (c.equals(trimmed)) {
                return c;
            }
        }
        for (String c : candidates) {
            if (trimmed.contains(c)) {
                return c;
            }
        }
        return candidates.get(0);
    }

    private List<String> parseStringList(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }
        // 优先尝试 JSON 数组
        try {
            JsonNode node = objectMapper.readTree(content);
            if (node.isArray()) {
                List<String> out = new ArrayList<>();
                node.forEach(n -> out.add(n.asText()));
                return out;
            }
        } catch (JsonProcessingException ignore) {
            // not JSON
        }
        // 退而求其次：抽取 "..."
        Matcher m = QUOTED_STRING.matcher(content);
        List<String> out = new ArrayList<>();
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    private List<RankedHit> parseRerank(String content, int totalCandidates) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }
        try {
            JsonNode node = objectMapper.readTree(extractJsonArrayOrObject(content));
            if (node.isArray()) {
                List<RankedHit> out = new ArrayList<>();
                node.forEach(n -> {
                    int idx = n.path("index").asInt(-1);
                    double score = n.path("score").asDouble(0d);
                    if (idx >= 0 && idx < totalCandidates) {
                        out.add(new RankedHit(idx, clamp(score)));
                    }
                });
                // 排序按分数降序
                out.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
                return out;
            }
        } catch (Exception e) {
            log.debug("[M3] rerank parse failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private QaResult parseQa(String content) {
        if (content == null || content.isBlank()) {
            return new QaResult("", Collections.emptyList());
        }
        try {
            JsonNode node = objectMapper.readTree(extractJsonArrayOrObject(content));
            String ans = node.path("answer").asText("");
            List<QaResult.Citation> cites = new ArrayList<>();
            JsonNode arr = node.path("citations");
            if (arr.isArray()) {
                arr.forEach(c -> {
                    int idx = c.path("index").asInt(-1);
                    String snippet = c.path("snippet").asText("");
                    cites.add(new QaResult.Citation(idx, snippet));
                });
            }
            return new QaResult(ans, cites);
        } catch (Exception e) {
            log.warn("[M3] qa parse failed: {}", e.getMessage());
            // 降级：把整个 content 当作 answer
            return new QaResult(content, Collections.emptyList());
        }
    }

    private String extractJsonArrayOrObject(String content) {
        // 先尝试整段解析
        String trimmed = content.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return trimmed;
        }
        // 抓首段 {...} 或 [...]
        Matcher m = JSON_OBJECT.matcher(trimmed);
        if (m.find()) {
            return m.group();
        }
        Matcher m2 = JSON_ARRAY.matcher(trimmed);
        if (m2.find()) {
            return m2.group();
        }
        return trimmed;
    }

    private double clamp(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0d;
        }
        if (v < 0d) {
            return 0d;
        }
        if (v > 1d) {
            return 1d;
        }
        return v;
    }

    // ============================ 流式问答 ============================

    @Override
    public Flux<String> answerStream(String question, List<String> context) {
        return answerStream(question, context, null);
    }

    /**
     * 用 java.net.http HttpClient 发送 SSE 流式请求，提取 token 并 emit。
     */
    private void doStreamAnswer(ChatRequest request, FluxSink<String> emitter) {
        String url = trimTrailingSlash(props.getBaseUrl()) + "/chat/completions";
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String jsonBody = objectMapper.writeValueAsString(request);

            var httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.resolveApiKey())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, "text/event-stream")
                    .timeout(java.time.Duration.ofMinutes(5))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            java.net.http.HttpResponse<java.io.InputStream> response = client.send(
                    httpRequest, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                emitter.error(new M3Exception("M3 HTTP " + response.statusCode()));
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring("data: ".length()).trim();
                        if ("[DONE]".equals(data)) {
                            emitter.next("[DONE]");
                            emitter.complete();
                            return;
                        }
                        try {
                            JsonNode chunk = objectMapper.readTree(data);
                            JsonNode delta = chunk.path("choices")
                                    .path(0)
                                    .path("delta")
                                    .path("content");
                            if (!delta.isMissingNode()) {
                                emitter.next(delta.asText());
                            }
                        } catch (JsonProcessingException e) {
                            log.debug("[M3] stream chunk parse failed: {}", data);
                        }
                    }
                }
            }
            if (!emitter.isCancelled()) {
                emitter.complete();
            }
        } catch (Exception e) {
            log.error("[M3] doStreamAnswer error: {}", e.getMessage());
            if (!emitter.isCancelled()) {
                emitter.error(e);
            }
        }
    }
}