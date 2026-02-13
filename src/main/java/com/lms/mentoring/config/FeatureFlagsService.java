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
 * The service binding provides credentials via VCAP_SERVICES environment variable.
 */
@Service
@Profile("cloud")
@Slf4j
public class FeatureFlagsService {
    
    public static final String FLAG_USE_DESTINATION_SMTP = "use-destination-smtp";
    
    private final String featureFlagsUri;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    public FeatureFlagsService(
            @Value("${sap.feature-flags.service.uri:#{null}}") String featureFlagsUri,
            @Value("${sap.feature-flags.service.clientid:#{null}}") String clientId,
            @Value("${sap.feature-flags.service.clientsecret:#{null}}") String clientSecret,
            @Value("${sap.feature-flags.service.url:#{null}}") String tokenUrl,
            ObjectMapper objectMapper) {
        this.featureFlagsUri = featureFlagsUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Evaluates a boolean feature flag.
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
        
        try {
            String accessToken = getAccessToken();
            
            String evaluationUrl = featureFlagsUri + "/api/v2/evaluate/" + flagName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
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
    
    private String getAccessToken() {
        if (clientId == null || clientSecret == null || tokenUrl == null) {
            throw new RuntimeException("Feature Flags service credentials not configured");
        }
        
        try {
            String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + credentials);
            
            String body = "grant_type=client_credentials";
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl + "/oauth/token",
                    HttpMethod.POST,
                    request,
                    String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Failed to get OAuth token: " + response.getStatusCode());
            }
            
            JsonNode tokenResponse = objectMapper.readTree(response.getBody());
            return tokenResponse.path("access_token").asText();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get OAuth token for Feature Flags service: " + e.getMessage(), e);
        }
    }
}