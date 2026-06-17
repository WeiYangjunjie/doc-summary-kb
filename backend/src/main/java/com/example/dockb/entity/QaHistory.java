package com.example.dockb.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答历史实体。
 *
 * <p>{@code citations} 用 JSON 字符串持久化（MySQL 8 JSON 列）。
 * <p>{@code ownerId} 记录提问者，未登录时为 null。
 */
@Data
@TableName("qa_history")
public class QaHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提问者 user.id，未登录为 null。 */
    private Long ownerId;

    private String question;

    private String answer;

    /** JSON 数组字符串。 */
    private String citations;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}