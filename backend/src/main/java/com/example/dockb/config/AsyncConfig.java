package com.example.dockb.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务执行器：摘要/分类异步任务共用。
 *
 * <p>线程池 core=2, max=4, queue=50；与契约第 7 节一致。
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Autowired
    private AppProperties appProperties;

    @Bean(name = "docKbExecutor")
    @Override
    public TaskExecutor getAsyncExecutor() {
        AppProperties.Async cfg = appProperties.getAsync();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cfg.getCorePoolSize());
        executor.setMaxPoolSize(cfg.getMaxPoolSize());
        executor.setQueueCapacity(cfg.getQueueCapacity());
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("doc-kb-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public java.util.concurrent.Executor getAsyncExecutorLegacy() {
        try {
            return getAsyncExecutor().getThreadPoolExecutor();
        } catch (RejectedExecutionException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}