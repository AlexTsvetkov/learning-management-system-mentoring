package com.lms.mentoring.notification.smtp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.security.xsuaa.client.OAuth2TokenResponse;
import com.sap.cloud.security.xsuaa.tokenflows.ClientCredentialsTokenFlow;
import com.sap.cloud.security.xsuaa.tokenflows.XsuaaTokenFlows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Base64;

/**
 * SMTP credentials provider that retrieves credentials from SAP BTP Destination Service.
 * 
 * This provider:
 * 1. Gets OAuth2 token from XSUAA service
 * 2. Calls Destination Service API to retrieve destination configuration
 * 3. Extracts SMTP credentials from the destination properties
 * 
 * The destination should be configured in SAP BTP Cockpit with:
 * - Type: MAIL
 * - Name: as configured in sap.smtp.destination.name
 * - Properties: mail.smtp.host, mail.smtp.port, mail.user, mail.password, mail.from
 * 
 * Retry is configured to handle transient network failures.
 */
@Component
@Profile("cloud")
@Slf4j
public class DestinationSmtpCredentialsProvider implements SmtpCredentialsProvider {
    
    private static final String PROVIDER_NAME = "SAP Destination Service";
    private static final String DEFAULT_FROM = "no-reply@lms.example.com";
    
    private final String destinationServiceUri;
    private final String destinationName;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    // VCAP credentials for destination service
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    
    public DestinationSmtpCredentialsProvider(
            @Value("${sap.destination.service.uri:#{null}}") String destinationServiceUri,
            @Value("${sap.smtp.destination.name:lms-smtp}") String destinationName,
            @Value("${sap.destination.service.clientid:#{null}}") String clientId,
            @Value("${sap.destination.service.clientsecret:#{null}}") String clientSecret,
            @Value("${sap.destination.service.url:#{null}}") String tokenUrl,
            ObjectMapper objectMapper) {
        this.destinationServiceUri = destinationServiceUri;
        this.destinationName = destinationName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    @Cacheable(value = "smtpCredentials", key = "'destination'")
    @Retryable(
            retryFor = {RestClientException.class, SmtpCredentialsException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public SmtpCredentials getCredentials() {
        log.debug("Retrieving SMTP credentials from Destination Service for destination: {}", destinationName);
        
        try {
            // Get OAuth2 token
            String accessToken = getAccessToken();
            
            // Call Destination Service API
            String destinationUrl = buildDestinationUrl();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    destinationUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new SmtpCredentialsException("Failed to retrieve destination: " + response.getStatusCode());
            }
            
            // Parse destination response
            return parseDestinationResponse(response.getBody());
            
        } catch (SmtpCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to retrieve SMTP credentials from Destination Service", e);
            throw new SmtpCredentialsException("Failed to retrieve SMTP credentials from Destination Service: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    private String getAccessToken() {
        if (clientId == null || clientSecret == null || tokenUrl == null) {
            throw new SmtpCredentialsException("Destination service credentials not configured (clientId, clientSecret, or tokenUrl missing)");
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
                throw new SmtpCredentialsException("Failed to get OAuth token: " + response.getStatusCode());
            }
            
            JsonNode tokenResponse = objectMapper.readTree(response.getBody());
            return tokenResponse.path("access_token").asText();
            
        } catch (SmtpCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new SmtpCredentialsException("Failed to get OAuth token: " + e.getMessage(), e);
        }
    }
    
    private String buildDestinationUrl() {
        if (destinationServiceUri == null || destinationServiceUri.isBlank()) {
            throw new SmtpCredentialsException("Destination service URI not configured");
        }
        return destinationServiceUri + "/destination-configuration/v1/destinations/" + destinationName;
    }
    
    private SmtpCredentials parseDestinationResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode destinationConfig = root.path("destinationConfiguration");
            
            if (destinationConfig.isMissingNode()) {
                throw new SmtpCredentialsException("No destinationConfiguration found in response");
            }
            
            // Extract SMTP properties from destination
            // SAP BTP Destination Service uses specific property names for mail destinations
            String host = getPropertyValue(destinationConfig, "mail.smtp.host", "URL");
            String portStr = getPropertyValue(destinationConfig, "mail.smtp.port", "587");
            String username = getPropertyValue(destinationConfig, "mail.user", "User");
            String password = getPropertyValue(destinationConfig, "mail.password", "Password");
            String from = destinationConfig.path("mail.from").asText(DEFAULT_FROM);
            
            // Handle URL format if host is a full URL
            if (host.startsWith("smtp://")) {
                host = host.substring(7);
            }
            if (host.contains(":")) {
                String[] parts = host.split(":");
                host = parts[0];
                if (parts.length > 1) {
                    portStr = parts[1];
                }
            }
            
            SmtpCredentials credentials = SmtpCredentials.builder()
                    .host(host)
                    .port(parsePort(portStr))
                    .username(username)
                    .password(password)
                    .from(from)
                    .build();
            
            if (!credentials.isValid()) {
                throw new SmtpCredentialsException("Invalid SMTP credentials from Destination Service");
            }
            
            log.info("Successfully retrieved SMTP credentials from Destination Service for host: {}", credentials.getHost());
            return credentials;
            
        } catch (SmtpCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new SmtpCredentialsException("Failed to parse destination response: " + e.getMessage(), e);
        }
    }
    
    private String getPropertyValue(JsonNode config, String... propertyNames) {
        for (String propName : propertyNames) {
            JsonNode value = config.path(propName);
            if (!value.isMissingNode() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        throw new SmtpCredentialsException("Required property not found: " + String.join(" or ", propertyNames));
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