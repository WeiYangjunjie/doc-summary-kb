package com.example.dockb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 文档摘要与知识库后端启动类。
 *
 * <p>启用 MyBatis-Plus Mapper 扫描与异步任务能力。
 */
@SpringBootApplication
@MapperScan("com.example.dockb.mapper")
@EnableAsync
public class DocKbApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocKbApplication.class, args);
    }
}