package com.example.dockb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档 VO（列表 / 详情通用）。
 *
 * <p>字段对齐契约 §5.1 DocumentVO。前端 HomeView、DocumentListView、DocumentDetailView 都消费这个 shape。
 */
@Data
public class DocumentVO {

    private Long id;

    private String title;

    private String originalName;

    /** pdf / txt / md / docx */
    private String fileType;

    private Long fileSize;

    /** AI 自动分类。默认 "未分类"。 */
    private String category;

    /** 英文逗号分隔的标签。 */
    private String tags;

    /** AI 摘要。 */
    private String summary;

    /** pending / processing / done / failed */
    private String status;

    /** 处理失败时的错误信息。 */
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
