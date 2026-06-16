package com.example.dockb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 引用条目 VO，QA 答案与搜索共用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitationVO {

    private Long documentId;
    private String title;
    private Long chunkId;
    private String snippet;
    private double score;
}