package com.example.dockb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 检索响应 VO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseVO {

    private String query;
    private List<SearchHitVO> hits;
}