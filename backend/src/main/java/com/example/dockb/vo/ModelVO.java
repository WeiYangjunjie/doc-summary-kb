package com.example.dockb.vo;

import lombok.Data;

/**
 * 模型信息 VO（对外暴露）。
 */
@Data
public class ModelVO {

    /** 模型标识名。 */
    private String name;

    /** 提供方。 */
    private String provider;

    /** 用户可见描述。 */
    private String description;

    /** 是否支持流式输出。 */
    private boolean supportsStream;

    /** 是否为当前激活模型。 */
    private boolean active;
}
