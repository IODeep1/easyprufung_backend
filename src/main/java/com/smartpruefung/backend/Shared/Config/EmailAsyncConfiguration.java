package com.smartpruefung.backend.Shared.Config;

import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

@Configuration
@EnableAsync
public class EmailAsyncConfiguration implements AsyncConfigurer {
    private static final Logger log = LoggerFactory.getLogger(EmailAsyncConfiguration.class);

    @Bean(name = "emailExecutor")
    public ThreadPoolTaskExecutor emailExecutor(
            @Value("${smartpruefung.email.workers:2}") int workers,
            @Value("${smartpruefung.email.queue-capacity:200}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, workers));
        executor.setMaxPoolSize(Math.max(1, workers));
        executor.setQueueCapacity(Math.max(0, queueCapacity));
        executor.setThreadNamePrefix("email-delivery-");
        // Never run SMTP on the registration thread when the queue is full.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (error, method, arguments) -> log.error(
                "Background email task failed in {} ({})", method.getName(), error.getClass().getSimpleName());
    }
}
