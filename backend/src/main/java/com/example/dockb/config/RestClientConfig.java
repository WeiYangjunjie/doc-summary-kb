package com.example.dockb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 调用 M3 的 RestClient Bean。
 *
 * <p>使用同步 {@link RestClient}，超时由底层 {@link SimpleClientHttpRequestFactory} 控制。
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient m3RestClient(M3Properties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMillis(props.getConnectTimeoutMs()).toMillis());
        factory.setReadTimeout((int) Duration.ofMillis(props.getReadTimeoutMs()).toMillis());
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}