package com.example.dockb.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 重排单条结果：原下标 + 模型打分 0~1。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankedHit {

    private int index;
    private double score;

    public static List<RankedHit> empty() {
        return List.of();
    }
}