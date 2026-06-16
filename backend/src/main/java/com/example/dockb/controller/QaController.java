package com.example.dockb.controller;

import com.example.dockb.common.Result;
import com.example.dockb.dto.QaAskRequest;
import com.example.dockb.service.QaService;
import com.example.dockb.vo.QaAnswerVO;
import com.example.dockb.vo.QaHistoryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 问答接口（契约 §5.4）。
 */
@RestController
@RequestMapping("/api/qa")
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/ask")
    public Result<QaAnswerVO> ask(@Valid @RequestBody QaAskRequest req) {
        return Result.success(qaService.ask(req.getQuestion(), req.getTopK()));
    }

    @GetMapping("/history")
    public Result<?> history(@RequestParam(defaultValue = "1") long page,
                             @RequestParam(defaultValue = "10") long size) {
        return Result.success(qaService.history(page, size));
    }
}