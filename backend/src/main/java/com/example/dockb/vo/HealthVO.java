package com.example.dockb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 健康检查响应 VO（契约 §5.5）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthVO {

    private String status;
    private boolean m3Reachable;
    private String m3Model;
}