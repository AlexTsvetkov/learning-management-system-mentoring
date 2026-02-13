package com.lms.mentoring.config;

import com.lms.mentoring.notification.smtp.SmtpCredentials;
import com.lms.mentoring.notification.smtp.SmtpCredentialsResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Dynamic mail configuration for cloud environment.
 * 
 * This configuration creates a JavaMailSender bean that retrieves SMTP credentials
 * dynamically from either:
 * - User-Provided Service (VCAP_SERVICES) when feature flag is disabled
 * - SAP Destination Service when feature flag is enabled
 * 
 * The credentials source is determined at runtime by the FeatureFlagsService.
 */
@Configuration
@Profile("cloud")
@Slf4j
public class DynamicMailConfig {
    
    private final SmtpCredentialsResolver credentialsResolver;
    
    public DynamicMailConfig(SmtpCredentialsResolver credentialsResolver) {
        this.credentialsResolver = credentialsResolver;
    }
    
    /**
     * Creates a JavaMailSender with dynamically resolved SMTP credentials.
     * The credentials are resolved at bean creation time and cached.
     * 
     * @return configured JavaMailSender
     */
    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        log.info("Configuring JavaMailSender with dynamic SMTP credentials");
        log.info("Active SMTP provider: {}", credentialsResolver.getActiveProviderName());
        
        SmtpCredentials credentials = credentialsResolver.resolveCredentials();
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(credentials.getHost());
        mailSender.setPort(credentials.getPort());
        mailSender.setUsername(credentials.getUsername());
        mailSender.setPassword(credentials.getPassword());
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        
        // For debugging in cloud
        props.put("mail.debug", "false");
        
        log.info("JavaMailSender configured successfully for host: {}:{}", 
                credentials.getHost(), credentials.getPort());
        
        return mailSender;
    }
    
    /**
     * Provides the 'from' address for emails.
     * Retrieved from SMTP credentials.
     * 
     * @return the from email address
     */
    @Bean
    public String mailFromAddress() {
        SmtpCredentials credentials = credentialsResolver.resolveCredentials();
        String from = credentials.getFrom();
        log.info("Mail from address configured: {}", from);
        return from;
    }
}