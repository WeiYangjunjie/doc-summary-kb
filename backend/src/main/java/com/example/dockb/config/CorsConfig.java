package com.example.dockb.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 配置：放行前端开发地址 http://localhost:5173。
 *
 * <p>使用 CorsFilter，比全局 WebMvcConfigurer 更稳妥。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Autowired
    private AppProperties appProperties;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 默认放行常见本地开发地址
        cfg.addAllowedOriginPattern("http://localhost:*");
        cfg.addAllowedOriginPattern("http://127.0.0.1:*");
        for (String o : appProperties.getCors().getAllowedOrigins()) {
            cfg.addAllowedOriginPattern(o);
        }
        cfg.addAllowedHeader("*");
        cfg.addAllowedMethod("*");
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(source);
    }
}