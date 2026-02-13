package com.lms.mentoring.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for the application.
 * 
 * Uses Caffeine as the cache provider with the following caches:
 * - smtpCredentials: Caches SMTP credentials for 1 hour
 * - featureFlags: Caches feature flag evaluations for 5 minutes
 * 
 * Also enables Spring Retry for retrying failed external API calls.
 */
@Configuration
@EnableCaching
@EnableRetry
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure individual caches with different TTLs
        cacheManager.registerCustomCache("smtpCredentials",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(10)
                        .recordStats()
                        .build());
        
        cacheManager.registerCustomCache("featureFlags",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .recordStats()
                        .build());
        
        // Default cache configuration for any other caches
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats());
        
        return cacheManager;
    }
}