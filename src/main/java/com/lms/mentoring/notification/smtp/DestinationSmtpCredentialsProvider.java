package com.lms.mentoring.notification.smtp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.mentoring.multitenancy.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * SMTP credentials provider that retrieves credentials from SAP BTP Destination Service.
 * 
 * This provider supports MULTITENANCY:
 * 1. For subscriber tenants: First tries to fetch destination from subscriber's subaccount
 * 2. If not found: Falls back to provider's subaccount
 * 
 * The destination should be configured in SAP BTP Cockpit with:
 * - Type: MAIL
 * - Name: as configured in sap.smtp.destination.name (e.g., "lms-smtp")
 * - Properties: mail.smtp.host, mail.smtp.port, mail.user, mail.password, mail.from
 * 
 * To configure subscriber-specific SMTP:
 * 1. Subscriber creates a Destination service instance in their subaccount
 * 2. Subscriber creates a destination with the same name (e.g., "lms-smtp")
 * 3. Application automatically uses subscriber's destination when they access the app
 * 
 * Retry is configured to handle transient network failures.
 */
@Component
@Profile("cloud")
@Slf4j
public class DestinationSmtpCredentialsProvider implements SmtpCredentialsProvider {
    
    private static final String PROVIDER_NAME = "SAP Destination Service (Tenant-Aware)";
    private static final String DEFAULT_FROM = "no-reply@lms.example.com";
    
    private final String destinationServiceUri;
    private final String destinationName;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final TenantAwareDestinationService tenantAwareDestinationService;
    
    // VCAP credentials for destination service
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    
    @Autowired
    public DestinationSmtpCredentialsProvider(
            @Value("${sap.destination.service.uri:#{null}}") String destinationServiceUri,
            @Value("${sap.smtp.destination.name:lms-smtp}") String destinationName,
            @Value("${sap.destination.service.clientid:#{null}}") String clientId,
            @Value("${sap.destination.service.clientsecret:#{null}}") String clientSecret,
            @Value("${sap.destination.service.url:#{null}}") String tokenUrl,
            ObjectMapper objectMapper,
            TenantAwareDestinationService tenantAwareDestinationService) {
        this.destinationServiceUri = destinationServiceUri;
        this.destinationName = destinationName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.tenantAwareDestinationService = tenantAwareDestinationService;
        
        log.info("DestinationSmtpCredentialsProvider initialized with tenant-aware destination lookup");
    }
    
    @Override
    @Cacheable(value = "smtpCredentials", key = "'destination-' + T(com.lms.mentoring.multitenancy.TenantContext).getCurrentTenant()")
    @Retryable(
            retryFor = {RestClientException.class, SmtpCredentialsException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public SmtpCredentials getCredentials() {
        String currentTenant = TenantContext.getCurrentTenant();
        log.info("Retrieving SMTP credentials from Destination Service for destination: {}, tenant: {}", 
                destinationName, currentTenant);
        
        try {
            // Use tenant-aware destination service to fetch destination
            // This will try subscriber's subaccount first, then fall back to provider
            JsonNode destinationResponse = tenantAwareDestinationService.getDestination(destinationName);
            
            if (destinationResponse == null) {
                throw new SmtpCredentialsException(
                        "Destination '" + destinationName + "' not found in any subaccount (tenant: " + currentTenant + ")");
            }
            
            // Parse destination response
            return parseDestinationResponse(destinationResponse);
            
        } catch (SmtpCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to retrieve SMTP credentials from Destination Service for tenant {}", currentTenant, e);
            throw new SmtpCredentialsException(
                    "Failed to retrieve SMTP credentials from Destination Service: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    /**
     * Parses the destination response from the Destination Service.
     * 
     * @param root the JSON response from the Destination Service
     * @return SmtpCredentials extracted from the destination
     */
    private SmtpCredentials parseDestinationResponse(JsonNode root) {
        try {
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