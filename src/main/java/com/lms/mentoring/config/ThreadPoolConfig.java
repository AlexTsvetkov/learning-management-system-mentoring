package com.lms.mentoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
public class ThreadPoolConfig {

    @Bean("emailNotificationExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
        t.setCorePoolSize(5);
        t.setMaxPoolSize(20);
        t.setQueueCapacity(50);
        t.setThreadNamePrefix("email-notification-executor-");
        t.initialize();
        return t;
    }
}
