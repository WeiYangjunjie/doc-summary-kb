package com.example.dockb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QA 答案响应 VO（与契约 §5.4 一致）。
 */
@Data
public class QaAnswerVO {

    private Long id;
    private String question;
    private String answer;
    private List<CitationVO> citations;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}