package com.example.dockb.service.impl;

import com.example.dockb.client.M3Client;
import com.example.dockb.client.M3Exception;
import com.example.dockb.client.dto.QaResult;
import com.example.dockb.client.dto.RankedHit;
import com.example.dockb.service.M3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * M3Service 默认实现：M3 失败时按关键字/启发式降级，不影响主流程。
 */
@Slf4j
@Service
public class M3ServiceImpl implements M3Service {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "is", "are", "was", "were",
            "的", "了", "和", "与", "或", "是", "在", "我", "你", "他", "她", "它", "们"));

    private final M3Client m3Client;

    public M3ServiceImpl(M3Client m3Client) {
        this.m3Client = m3Client;
    }

    @Override
    public boolean isReachable() {
        try {
            return m3Client.ping();
        } catch (Exception e) {
            log.debug("[M3Service] ping error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String classifyWithFallback(String text, List<String> candidates, String defaultCategory) {
        try {
            return m3Client.classify(text, candidates);
        } catch (M3Exception | RuntimeException e) {
            log.warn("[M3Service] classify fallback: {}", e.getMessage());
            return classifyByKeyword(text, candidates, defaultCategory);
        }
    }

    @Override
    public String summarizeWithFallback(String text, String defaultSummary) {
        try {
            String s = m3Client.summarize(text);
            if (s != null && !s.isBlank()) {
                return s;
            }
        } catch (M3Exception | RuntimeException e) {
            log.warn("[M3Service] summarize fallback: {}", e.getMessage());
        }
        return defaultSummary != null ? defaultSummary : truncate(text, 300);
    }

    @Override
    public List<String> extractTagsWithFallback(String text, List<String> defaults) {
        try {
            List<String> tags = m3Client.extractTags(text);
            if (tags != null && !tags.isEmpty()) {
                return tags;
            }
        } catch (M3Exception | RuntimeException e) {
            log.warn("[M3Service] extractTags fallback: {}", e.getMessage());
        }
        return defaults != null ? defaults : Collections.emptyList();
    }

    @Override
    public List<RankedHit> rerankWithFallback(String query, List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<RankedHit> ranked = m3Client.rerank(query, candidates);
            if (ranked != null && !ranked.isEmpty()) {
                return ranked;
            }
        } catch (M3Exception | RuntimeException e) {
            log.warn("[M3Service] rerank fallback: {}", e.getMessage());
        }
        return tfRank(query, candidates);
    }

    @Override
    public QaResult answerWithFallback(String question, List<String> context) {
        if (context == null || context.isEmpty()) {
            return new QaResult("知识库中暂无相关资料。", Collections.emptyList());
        }
        try {
            QaResult r = m3Client.answer(question, context);
            if (r != null && r.getAnswer() != null && !r.getAnswer().isBlank()) {
                return r;
            }
        } catch (M3Exception | RuntimeException e) {
            log.warn("[M3Service] answer fallback: {}", e.getMessage());
        }
        // 降级：拼接 top2 上下文作为答案
        List<QaResult.Citation> cites = new ArrayList<>();
        StringBuilder sb = new StringBuilder("根据本地资料：\n");
        int limit = Math.min(2, context.size());
        for (int i = 0; i < limit; i++) {
            cites.add(new QaResult.Citation(i, truncate(context.get(i), 200)));
            sb.append("- ").append(truncate(context.get(i), 300)).append("\n");
        }
        return new QaResult(sb.toString(), cites);
    }

    // ----------------------------- 降级策略 -----------------------------

    private String classifyByKeyword(String text, List<String> candidates, String defaultCategory) {
        if (candidates == null || candidates.isEmpty()) {
            return defaultCategory == null ? "未分类" : defaultCategory;
        }
        if (text == null || text.isBlank()) {
            return candidates.get(0);
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Map<String, Integer> hit = new HashMap<>();
        for (String c : candidates) {
            int n = countOccurrences(lower, c.toLowerCase(Locale.ROOT));
            if (n > 0) {
                hit.put(c, n);
            }
        }
        if (hit.isEmpty()) {
            return defaultCategory != null ? defaultCategory : candidates.get(0);
        }
        return hit.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(candidates.get(0));
    }

    private int countOccurrences(String text, String token) {
        if (token == null || token.isEmpty()) {
            return 0;
        }
        int idx = 0;
        int count = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }

    private List<RankedHit> tfRank(String query, List<String> candidates) {
        List<String> tokens = tokenize(query);
        if (tokens.isEmpty()) {
            return Collections.emptyList();
        }
        List<RankedHit> out = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            String cand = candidates.get(i);
            double score = tf(tokens, cand);
            out.add(new RankedHit(i, score));
        }
        out.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return out;
    }

    private List<String> tokenize(String s) {
        if (s == null || s.isBlank()) {
            return Collections.emptyList();
        }
        String[] parts = s.toLowerCase(Locale.ROOT).split("[\\s,，。.;；:：!?？()()\\[\\]【】\"'\\-]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty() || STOPWORDS.contains(t)) {
                continue;
            }
            out.add(t);
        }
        return out;
    }

    private double tf(List<String> tokens, String text) {
        if (text == null || text.isEmpty() || tokens.isEmpty()) {
            return 0d;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int total = 0;
        for (String t : tokens) {
            total += countOccurrences(lower, t);
        }
        // 简单归一：相对长度
        return (double) total / Math.max(1, text.length() / 100);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}