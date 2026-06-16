package com.example.dockb.controller;

import com.example.dockb.client.M3Client;
import com.example.dockb.common.Result;
import com.example.dockb.dto.QaAskRequest;
import com.example.dockb.service.QaService;
import com.example.dockb.vo.QaAnswerVO;
import com.example.dockb.vo.QaHistoryVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 问答接口（契约 §5.4）。
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
public class QaController {

    private final QaService qaService;
    private final M3Client m3Client;

    public QaController(QaService qaService, M3Client m3Client) {
        this.qaService = qaService;
        this.m3Client = m3Client;
    }

    @PostMapping("/ask")
    public Result<QaAnswerVO> ask(@Valid @RequestBody QaAskRequest req) {
        return Result.success(qaService.ask(req.getQuestion(), req.getTopK()));
    }

    /**
     * 流式问答 SSE 端点。
     * 逐 token 返回 M3 生成内容，前端可逐字显示。
     * 最终发送 [DONE] 后，异步将完整答案存入历史。
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@Valid @RequestBody QaAskRequest req) {
        log.info("[QA] stream ask: question={}, topK={}", req.getQuestion(), req.getTopK());
        // buildContext 直接返回上下文字符串列表
        List<String> context = qaService.buildContext(req.getQuestion(), req.getTopK());

        Flux<String> stream = m3Client.answerStream(req.getQuestion(), context);

        // 收集完整答案，结束后异步存库
        StringBuilder fullAnswer = new StringBuilder();
        return stream
                .doOnNext(token -> {
                    if (!"[DONE]".equals(token)) {
                        fullAnswer.append(token);
                    }
                })
                .doOnComplete(() -> {
                    String answer = fullAnswer.toString();
                    log.info("[QA] stream done, answer length={}", answer.length());
                    // 异步存历史（不阻塞响应）
                    try {
                        qaService.saveHistoryAsync(req.getQuestion(), answer);
                    } catch (Exception e) {
                        log.warn("[QA] save history async failed: {}", e.getMessage());
                    }
                });
    }

    @GetMapping("/history")
    public Result<?> history(@RequestParam(defaultValue = "1") long page,
                             @RequestParam(defaultValue = "10") long size) {
        return Result.success(qaService.history(page, size));
    }
}