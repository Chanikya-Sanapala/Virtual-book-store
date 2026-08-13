package com.bookstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "importTaskExecutor")
    public Executor importTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("import-task-");
        executor.setRejectedExecutionHandler((r, exec) -> {
            logger.error("Import Task Rejected: Thread pool and queue are full.");
            throw new RuntimeException("Server is busy processing imports. Please try again later.");
        });
        executor.initialize();
        logger.info("Initialized importTaskExecutor with corePoolSize=4, maxPoolSize=10, queueCapacity=50");
        return executor;
    }
}
