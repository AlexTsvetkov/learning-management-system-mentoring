package com.lms.mentoring.notification.smtp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * SMTP credentials provider that reads credentials from VCAP_SERVICES environment variable.
 * This is used when the application is running on SAP BTP Cloud Foundry with a user-provided service.
 * 
 * The user-provided service should contain the following JSON structure:
 * {
 *   "host": "smtp.example.com",
 *   "port": "587",
 *   "username": "user@example.com",
 *   "password": "secret",
 *   "from": "no-reply@example.com"
 * }
 */
@Component
@Profile("cloud")
@Slf4j
public class VcapSmtpCredentialsProvider implements SmtpCredentialsProvider {
    
    private static final String PROVIDER_NAME = "VCAP_SERVICES (User-Provided)";
    private static final String DEFAULT_FROM = "no-reply@lms.example.com";
    
    private final String vcapServices;
    private final String serviceName;
    private final ObjectMapper objectMapper;
    
    public VcapSmtpCredentialsProvider(
            @Value("${VCAP_SERVICES:#{null}}") String vcapServices,
            @Value("${sap.smtp.user-provided-service.name:lms-smtp-credentials}") String serviceName,
            ObjectMapper objectMapper) {
        this.vcapServices = vcapServices;
        this.serviceName = serviceName;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Cacheable(value = "smtpCredentials", key = "'vcap'")
    public SmtpCredentials getCredentials() {
        log.debug("Retrieving SMTP credentials from VCAP_SERVICES for service: {}", serviceName);
        
        if (vcapServices == null || vcapServices.isBlank()) {
            throw new SmtpCredentialsException("VCAP_SERVICES environment variable is not set");
        }
        
        try {
            JsonNode root = objectMapper.readTree(vcapServices);
            
            // Look for user-provided services
            JsonNode userProvided = root.get("user-provided");
            if (userProvided == null || !userProvided.isArray()) {
                throw new SmtpCredentialsException("No user-provided services found in VCAP_SERVICES");
            }
            
            // Find our SMTP service by name
            for (JsonNode service : userProvided) {
                String name = service.path("name").asText();
                if (serviceName.equals(name)) {
                    JsonNode credentials = service.get("credentials");
                    if (credentials == null) {
                        throw new SmtpCredentialsException("No credentials found in service: " + serviceName);
                    }
                    
                    SmtpCredentials smtpCredentials = SmtpCredentials.builder()
                            .host(getRequiredField(credentials, "host"))
                            .port(parsePort(credentials.path("port").asText("587")))
                            .username(getRequiredField(credentials, "username"))
                            .password(getRequiredField(credentials, "password"))
                            .from(credentials.path("from").asText(DEFAULT_FROM))
                            .build();
                    
                    if (!smtpCredentials.isValid()) {
                        throw new SmtpCredentialsException("Invalid SMTP credentials from VCAP_SERVICES");
                    }
                    
                    log.info("Successfully retrieved SMTP credentials from VCAP_SERVICES for host: {}", 
                            smtpCredentials.getHost());
                    return smtpCredentials;
                }
            }
            
            throw new SmtpCredentialsException("SMTP service not found in VCAP_SERVICES: " + serviceName);
            
        } catch (SmtpCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new SmtpCredentialsException("Failed to parse VCAP_SERVICES: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    private String getRequiredField(JsonNode credentials, String fieldName) {
        JsonNode field = credentials.get(fieldName);
        if (field == null || field.asText().isBlank()) {
            throw new SmtpCredentialsException("Missing required field in SMTP credentials: " + fieldName);
        }
        return field.asText();
    }
    
    private int parsePort(String portStr) {
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            log.warn("Invalid port '{}', using default 587", portStr);
            return 587;
        }
    }
}