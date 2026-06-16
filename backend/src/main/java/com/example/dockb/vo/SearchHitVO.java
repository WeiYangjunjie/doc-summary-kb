package com.example.dockb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索命中条目 VO。
 *
 * <p>字段与契约 §5.3 一致。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchHitVO {

    private Long documentId;
    private String title;
    private String category;
    /** 0~1。 */
    private double score;
    /** 命中前后各 80 字。 */
    private String snippet;
    private Long chunkId;
}