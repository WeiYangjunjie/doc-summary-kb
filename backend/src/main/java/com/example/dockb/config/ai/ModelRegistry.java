package com.example.dockb.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AI 模型注册表：管理可用模型列表与当前激活模型。
 *
 * <p>模型在 application.yml 的 {@code ai.models} 下配置。
 * 切换后自动生效（线程安全）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class ModelRegistry {

    /** 当前激活的模型名。 */
    private String activeModel = "MiniMax-M3";

    /** 可用模型列表。 */
    private List<ModelInfo> models = new CopyOnWriteArrayList<>();

    public ModelInfo getActiveModelInfo() {
        return models.stream()
                .filter(m -> m.getName().equals(activeModel))
                .findFirst()
                .orElseGet(() -> {
                    if (!models.isEmpty()) {
                        return models.get(0);
                    }
                    // fallback: 构造一个默认 info
                    ModelInfo fallback = new ModelInfo();
                    fallback.setName(activeModel);
                    fallback.setProvider("MiniMax");
                    fallback.setDescription("默认模型");
                    fallback.setSupportsStream(true);
                    return fallback;
                });
    }

    public void switchTo(String modelName) {
        boolean found = models.stream().anyMatch(m -> m.getName().equals(modelName));
        if (found) {
            this.activeModel = modelName;
        } else {
            throw new IllegalArgumentException("Unknown model: " + modelName);
        }
    }

    public boolean isSupportsStream(String modelName) {
        return models.stream()
                .filter(m -> m.getName().equals(modelName))
                .findFirst()
                .map(ModelInfo::isSupportsStream)
                .orElse(true);
    }

    @Data
    public static class ModelInfo {
        /** 模型标识名（传给 API 的 model 参数）。 */
        private String name;

        /** 提供方：MiniMax / OpenAI / Groq 等。 */
        private String provider;

        /** 用户可见的中文描述。 */
        private String description;

        /** 是否支持流式输出。 */
        private boolean supportsStream = true;
    }
}
