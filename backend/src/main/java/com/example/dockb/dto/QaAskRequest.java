package com.example.dockb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * QA 请求体（契约 §5.4）。
 */
@Data
public class QaAskRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题不能超过 2000 字")
    private String question;

    /** 默认 5，范围 [1, 20]。 */
    private Integer topK;
}