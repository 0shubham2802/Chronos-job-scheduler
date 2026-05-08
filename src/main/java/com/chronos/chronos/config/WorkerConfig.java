package com.chronos.chronos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class WorkerConfig {

    // This is the thread pool that executes jobs
    // Think of it as having 8-32 workers ready to pick up and run jobs
    @Bean
    public ThreadPoolTaskExecutor jobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Always keep 8 threads alive — ready to pick up jobs immediately
        executor.setCorePoolSize(8);

        // Under heavy load, scale up to 32 threads
        executor.setMaxPoolSize(32);

        // If all threads are busy, queue up to 500 jobs
        executor.setQueueCapacity(500);

        // CallerRunsPolicy = if queue is full and max threads reached,
        // the calling thread (Quartz) runs the job itself
        // This provides backpressure instead of silently dropping jobs
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // Thread names show up in logs — makes debugging much easier
        executor.setThreadNamePrefix("chronos-worker-");

        // Wait for running jobs to finish before shutting down
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}
