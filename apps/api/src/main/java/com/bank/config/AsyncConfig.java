package com.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Bounded thread pools for {@code @Async} work. Two pools keep concerns isolated:
 * a general pool for short side-effects (KYC/notifications) and a dedicated,
 * tightly-bounded pool for exports so a burst of large CSV jobs can neither spawn
 * unlimited threads nor starve the rest of the app.
 *
 * <p>The export pool uses {@link ThreadPoolExecutor.AbortPolicy}: once the pool
 * and its queue are full, new submissions are rejected rather than run on the
 * caller (the request thread) — the caller marks the job FAILED so it is never
 * left stuck in QUEUED. A durable queue / reaper is the next step for restart
 * safety.
 */
@Configuration
public class AsyncConfig {

    /** Default executor for general @Async side-effects. */
    @Bean("taskExecutor")
    TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("async-");
        ex.initialize();
        return ex;
    }

    /** Isolated, bounded pool for transaction CSV exports. */
    @Bean("exportExecutor")
    TaskExecutor exportExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("export-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        ex.initialize();
        return ex;
    }
}
