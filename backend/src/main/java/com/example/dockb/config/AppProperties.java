package com.example.dockb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用级自定义配置。
 *
 * <p>对应 application.yml 中 {@code app.*} 节点。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 文件本地保存目录，相对 user.dir。 */
    private String uploadDir = "./uploads";

    /** 分块大小（字符数）。 */
    private int chunkSize = 1000;

    private Search search = new Search();
    private Async async = new Async();
    private Cors cors = new Cors();

    @Data
    public static class Search {
        /** 关键词检索候选最大数。 */
        private int maxCandidates = 30;
        /** 摘要片段半径。 */
        private int snippetRadius = 80;
    }

    @Data
    public static class Async {
        private int corePoolSize = 2;
        private int maxPoolSize = 4;
        private int queueCapacity = 50;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }
}