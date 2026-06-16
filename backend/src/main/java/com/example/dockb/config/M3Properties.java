package com.example.dockb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MiniMax M3 调用相关配置。
 *
 * <p>key 读取顺序：环境变量 {@code MiniMax_API_KEY} → {@code MiniMax.api-key}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "MiniMax")
public class M3Properties {

    /** 兼容 OpenAI 协议；启动时会先尝试环境变量覆盖。 */
    private String apiKey = "REPLACE_WITH_YOUR_KEY";

    private String baseUrl = "https://api.MiniMax.chat/v1";
    private String model = "MiniMax-M3";
    private int connectTimeoutMs = 10_000;
    private int readTimeoutMs = 60_000;
    private int maxRetries = 1;

    /**
     * 解析后的最终 key（环境变量优先）。
     */
    public String resolveApiKey() {
        String env = System.getenv("MiniMax_API_KEY");
        if (env != null && !env.isBlank() && !"REPLACE_WITH_YOUR_KEY".equals(env)) {
            return env;
        }
        return apiKey;
    }

    public boolean isKeyConfigured() {
        String k = resolveApiKey();
        return k != null && !k.isBlank() && !"REPLACE_WITH_YOUR_KEY".equals(k);
    }
}