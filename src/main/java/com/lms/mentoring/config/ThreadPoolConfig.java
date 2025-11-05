package com.lms.mentoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
public class ThreadPoolConfig {

    @Bean("emailNotificationExecutor")
    @ConfigurationProperties(prefix = "app.email-executor")
    public Executor emailExecutor() {
        // properties are bound automatically from YAML
        return new ThreadPoolTaskExecutor();
    }
}
