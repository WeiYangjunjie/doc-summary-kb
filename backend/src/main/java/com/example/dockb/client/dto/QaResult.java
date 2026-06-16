package com.example.dockb.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * QA 答案 + 引用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaResult {

    private String answer;
    private List<Citation> citations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private int index;
        private String snippet;
    }
}