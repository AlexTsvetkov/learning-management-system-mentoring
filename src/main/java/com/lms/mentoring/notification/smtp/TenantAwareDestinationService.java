package com.lms.mentoring.notification.smtp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.mentoring.multitenancy.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Tenant-aware service for fetching destinations from SAP BTP Destination Service.
 * 
 * Implements the multitenancy strategy for destination retrieval:
 * 1. First, try to fetch the destination from the SUBSCRIBER's subaccount
 * 2. If not found (404), fall back to the PROVIDER's subaccount
 * 
 * This allows subscribers to configure their own SMTP destinations while
 * falling back to the provider's default configuration.
 * 
 * To use subscriber-level destinations:
 * 1. Subscriber creates a Destination service instance in their subaccount
 * 2. Subscriber creates a destination with the same name (e.g., "lms-smtp")
 * 3. The application will use the subscriber's destination when they access the app
 */
@Service
@Profile("cloud")
@Slf4j
public class TenantAwareDestinationService {

    private final String destinationServiceUri;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    private final String providerTenantId;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public TenantAwareDestinationService(
            @Value("${sap.destination.service.uri:#{null}}") String destinationServiceUri,
            @Value("${sap.destination.service.clientid:#{null}}") String clientId,
            @Value("${sap.destination.service.clientsecret:#{null}}") String clientSecret,
            @Value("${sap.destination.service.url:#{null}}") String tokenUrl,
            @Value("${vcap.services.lms-xsuaa.credentials.zoneid:#{null}}") String providerTenantId,
            ObjectMapper objectMapper) {
        this.destinationServiceUri = destinationServiceUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.providerTenantId = providerTenantId;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        
        log.info("TenantAwareDestinationService initialized. Provider tenant: {}", providerTenantId);
    }

    /**
     * Fetches a destination by name with tenant-aware lookup.
     * 
     * Strategy:
     * 1. If current tenant is a subscriber, try subscriber's subaccount first
     * 2. If not found or if provider tenant, fetch from provider's subaccount
     * 
     * @param destinationName the name of the destination to fetch
     * @return the destination configuration as JSON, or null if not found
     */
    public JsonNode getDestination(String destinationName) {
        String currentTenant = TenantContext.getCurrentTenant();
        boolean isSubscriber = !TenantContext.DEFAULT_TENANT.equals(currentTenant) 
                && !currentTenant.equals(providerTenantId);
        
        log.info("Fetching destination '{}' for tenant: {} (isSubscriber: {})", 
                destinationName, currentTenant, isSubscriber);
        
        if (isSubscriber) {
            // Try subscriber's subaccount first
            log.info("Attempting to fetch destination '{}' from subscriber subaccount: {}", 
                    destinationName, currentTenant);
            
            try {
                JsonNode destination = fetchDestinationForTenant(destinationName, currentTenant);
                if (destination != null) {
                    log.info("Found destination '{}' in subscriber subaccount: {}", 
                            destinationName, currentTenant);
                    return destination;
                }
            } catch (DestinationNotFoundException e) {
                log.info("Destination '{}' not found in subscriber subaccount {}, falling back to provider", 
                        destinationName, currentTenant);
            } catch (Exception e) {
                log.warn("Error fetching destination '{}' from subscriber subaccount {}: {}. Falling back to provider.", 
                        destinationName, currentTenant, e.getMessage());
            }
        }
        
        // Fall back to provider's subaccount
        log.info("Fetching destination '{}' from provider subaccount", destinationName);
        try {
            return fetchDestinationForTenant(destinationName, null); // null = use default (provider) token
        } catch (DestinationNotFoundException e) {
            log.error("Destination '{}' not found in provider subaccount either", destinationName);
            return null;
        }
    }

    /**
     * Fetches a destination for a specific tenant.
     * 
     * @param destinationName the destination name
     * @param tenantId the tenant ID to fetch for, or null for provider
     * @return the destination configuration
     * @throws DestinationNotFoundException if the destination is not found
     */
    private JsonNode fetchDestinationForTenant(String destinationName, String tenantId) {
        if (destinationServiceUri == null || destinationServiceUri.isBlank()) {
            throw new SmtpCredentialsException("Destination service URI not configured");
        }
        
        String accessToken = getAccessToken(tenantId);
        String url = destinationServiceUri + "/destination-configuration/v1/destinations/" + destinationName;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // For subscriber-level destinations, we need to pass the tenant context
        if (tenantId != null && !tenantId.equals(providerTenantId)) {
            // The X-tenant-id header tells the Destination Service which tenant's destinations to look up
            headers.set("X-tenant-id", tenantId);
            log.info("Added X-tenant-id header: {}", tenantId);
        }
        
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            }
            
            throw new DestinationNotFoundException("Empty response for destination: " + destinationName);
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new DestinationNotFoundException("Destination not found: " + destinationName);
        } catch (RestClientException e) {
            log.error("Failed to fetch destination '{}': {}", destinationName, e.getMessage());
            throw new SmtpCredentialsException("Failed to fetch destination: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching destination '{}': {}", destinationName, e.getMessage());
            throw new SmtpCredentialsException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Gets an OAuth2 access token for the Destination Service.
     * 
     * For subscriber-level access, we need to exchange the token with the subscriber's tenant context.
     * 
     * @param tenantId the tenant ID, or null for provider token
     * @return the access token
     */
    private String getAccessToken(String tenantId) {
        if (clientId == null || clientSecret == null || tokenUrl == null) {
            throw new SmtpCredentialsException(
                    "Destination service credentials not configured (clientId, clientSecret, or tokenUrl missing)");
        }
        
        try {
            String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + credentials);
            
            // For subscriber-level token, we need to request on behalf of the subscriber
            // Using the X-zid header or subdomain in token URL
            String effectiveTokenUrl = tokenUrl;
            if (tenantId != null && !tenantId.equals(providerTenantId)) {
                // For subscriber tokens, we might need to use their XSUAA URL
                // However, with XSUAA broker plan, we can use the same credentials
                // and the destination service will handle tenant context via X-tenant-id header
                log.info("Getting token for subscriber tenant: {}", tenantId);
            }
            
            String body = "grant_type=client_credentials";
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    effectiveTokenUrl + "/oauth/token",
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

    /**
     * Exception thrown when a destination is not found in a specific subaccount.
     */
    public static class DestinationNotFoundException extends RuntimeException {
        public DestinationNotFoundException(String message) {
            super(message);
        }
    }
}