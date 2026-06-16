package com.example.dockb.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 ChatCompletion 请求体（已简化）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    private String model;
    private List<Message> messages;

    /** 是否启用流式输出（SSE）。 */
    private Boolean stream;

    /** 可选：覆盖默认 temperature。 */
    private Double temperature;

    /** 期望模型返回 JSON 时，配合 prompt 提示使用。 */
    private Boolean jsonMode;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        /** system / user / assistant。 */
        private String role;
        private String content;

        /** 可选 name。 */
        private String name;

        /** 可选函数调用相关字段。 */
        private Map<String, Object> extra;
    }
}