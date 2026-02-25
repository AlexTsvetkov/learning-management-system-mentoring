package com.lms.mentoring.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Service for interacting with SAP BTP Feature Flags Service.
 * 
 * This service retrieves feature flag values from the Feature Flags Service REST API.
 * Feature flags allow runtime control of application behavior without code changes.
 * 
 * The 'lite' plan uses Basic Auth (username/password) for authentication.
 * The service binding provides credentials via VCAP_SERVICES environment variable.
 */
@Service
@Profile("cloud")
@Slf4j
public class FeatureFlagsService {
    
    public static final String FLAG_USE_DESTINATION_SMTP = "use-destination-smtp";
    
    private final String featureFlagsUri;
    private final String username;
    private final String password;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    public FeatureFlagsService(
            @Value("${sap.feature-flags.service.uri:#{null}}") String featureFlagsUri,
            @Value("${sap.feature-flags.service.username:#{null}}") String username,
            @Value("${sap.feature-flags.service.password:#{null}}") String password,
            ObjectMapper objectMapper) {
        this.featureFlagsUri = featureFlagsUri;
        this.username = username;
        this.password = password;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        
        log.info("FeatureFlagsService initialized with URI: {}, username configured: {}", 
                featureFlagsUri, username != null && !username.isEmpty());
    }
    
    /**
     * Evaluates a boolean feature flag using Basic Auth.
     * 
     * @param flagName the name of the feature flag
     * @param defaultValue the default value if the flag cannot be evaluated
     * @return the flag value, or defaultValue if evaluation fails
     */
    @Cacheable(value = "featureFlags", key = "#flagName")
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public boolean isEnabled(String flagName, boolean defaultValue) {
        log.debug("Evaluating feature flag: {}", flagName);
        
        if (featureFlagsUri == null || featureFlagsUri.isBlank()) {
            log.warn("Feature Flags service URI not configured, using default value: {}", defaultValue);
            return defaultValue;
        }
        
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("Feature Flags service credentials not configured, using default value: {}", defaultValue);
            return defaultValue;
        }
        
        try {
            String evaluationUrl = featureFlagsUri + "/api/v2/evaluate/" + flagName;
            
            // Create Basic Auth header
            String credentials = username + ":" + password;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedCredentials);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    evaluationUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Failed to evaluate feature flag {}: {}, using default: {}", 
                        flagName, response.getStatusCode(), defaultValue);
                return defaultValue;
            }
            
            JsonNode result = objectMapper.readTree(response.getBody());
            boolean flagValue = result.path("variation").asBoolean(defaultValue);
            
            log.info("Feature flag '{}' evaluated to: {}", flagName, flagValue);
            return flagValue;
            
        } catch (Exception e) {
            log.error("Error evaluating feature flag '{}', using default value: {}", flagName, defaultValue, e);
            return defaultValue;
        }
    }
    
    /**
     * Checks if the use-destination-smtp feature flag is enabled.
     * When enabled, SMTP credentials are retrieved from Destination Service.
     * When disabled, SMTP credentials are retrieved from User-Provided Service (VCAP).
     * 
     * @return true if destination-based SMTP should be used
     */
    public boolean isUseDestinationSmtpEnabled() {
        return isEnabled(FLAG_USE_DESTINATION_SMTP, false);
    }
}