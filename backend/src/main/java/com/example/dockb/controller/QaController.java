package com.example.dockb.controller;

import com.example.dockb.common.Result;
import com.example.dockb.dto.QaAskRequest;
import com.example.dockb.service.QaService;
import com.example.dockb.vo.QaAnswerVO;
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

/**
 * 问答接口（契约 §5.4）。
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/ask")
    public Result<QaAnswerVO> ask(@Valid @RequestBody QaAskRequest req) {
        log.info("[QA] ask: question={}, topK={}, model={}", req.getQuestion(), req.getTopK(), req.getModel());
        return Result.success(qaService.ask(req.getQuestion(), req.getTopK(), req.getModel()));
    }

    /**
     * 流式问答 SSE 端点。
     * 逐 token 返回 M3 生成内容，前端可逐字显示。
     * 最终发送 [DONE] 后，异步将完整答案存入历史。
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@Valid @RequestBody QaAskRequest req) {
        log.info("[QA] stream ask: question={}, topK={}, model={}", req.getQuestion(), req.getTopK(), req.getModel());
        Flux<String> stream = qaService.askStream(req.getQuestion(), req.getTopK(), req.getModel());

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
