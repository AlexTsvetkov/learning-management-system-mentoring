package com.lms.mentoring.multitenancy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing tenant database schemas via SAP Service Manager API.
 * 
 * Service Manager is used to dynamically create and delete HANA service instances
 * (schemas) for each tenant during subscription/unsubscription.
 * 
 * Flow:
 * 1. On subscription: Create a new HANA schema service instance for the tenant
 * 2. Store the schema credentials for the tenant's connection pool
 * 3. On unsubscription: Delete the tenant's HANA schema service instance
 */
@Slf4j
@Service
@Profile("cloud")
@RequiredArgsConstructor
public class ServiceManagerSchemaService {

    private final RestTemplate restTemplate = new RestTemplate();
    
    // Service Manager credentials from VCAP_SERVICES
    private ServiceManagerCredentials credentials;
    private String accessToken;
    private long tokenExpiry;

    /**
     * Creates a HANA schema for a tenant via Service Manager.
     * 
     * @param tenantId the tenant ID
     * @param subdomain the tenant subdomain
     * @return the service instance ID
     */
    public String createTenantSchema(String tenantId, String subdomain) {
        log.info("Creating HANA schema via Service Manager for tenant: {}", tenantId);
        
        ensureCredentials();
        String token = getAccessToken();
        
        String instanceName = "lms-hana-" + tenantId.substring(0, 8);
        
        // Check if instance already exists
        Optional<String> existingInstance = findServiceInstance(token, instanceName);
        if (existingInstance.isPresent()) {
            log.info("Schema already exists for tenant {}: {}", tenantId, existingInstance.get());
            return existingInstance.get();
        }
        
        // Create new service instance
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        
        // Service instance parameters for HANA schema
        Map<String, Object> parameters = Map.of(
            "database_id", getHanaDatabaseId(),
            "schema", TenantContext.toSchemaName(tenantId)
        );
        
        Map<String, Object> body = Map.of(
            "name", instanceName,
            "service_offering_name", "hana",
            "service_plan_name", "schema",
            "parameters", parameters,
            "labels", Map.of(
                "tenant_id", List.of(tenantId),
                "subdomain", List.of(subdomain)
            )
        );
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<ServiceInstanceResponse> response = restTemplate.exchange(
                credentials.getSmUrl() + "/v1/service_instances",
                HttpMethod.POST,
                request,
                ServiceInstanceResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String instanceId = response.getBody().getId();
                log.info("Created HANA schema instance {} for tenant {}", instanceId, tenantId);
                return instanceId;
            } else {
                throw new RuntimeException("Failed to create schema: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to create HANA schema for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to create tenant schema via Service Manager", e);
        }
    }

    /**
     * Deletes a tenant's HANA schema via Service Manager.
     * 
     * @param tenantId the tenant ID
     */
    public void deleteTenantSchema(String tenantId) {
        log.info("Deleting HANA schema via Service Manager for tenant: {}", tenantId);
        
        ensureCredentials();
        String token = getAccessToken();
        
        String instanceName = "lms-hana-" + tenantId.substring(0, 8);
        
        Optional<String> instanceId = findServiceInstance(token, instanceName);
        if (instanceId.isEmpty()) {
            log.warn("No schema found for tenant {}", tenantId);
            return;
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange(
                credentials.getSmUrl() + "/v1/service_instances/" + instanceId.get(),
                HttpMethod.DELETE,
                request,
                Void.class
            );
            
            log.info("Deleted HANA schema instance {} for tenant {}", instanceId.get(), tenantId);
        } catch (Exception e) {
            log.error("Failed to delete HANA schema for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete tenant schema via Service Manager", e);
        }
    }

    /**
     * Checks if a schema exists for a tenant.
     */
    public boolean schemaExists(String tenantId) {
        try {
            ensureCredentials();
            String token = getAccessToken();
            String instanceName = "lms-hana-" + tenantId.substring(0, 8);
            return findServiceInstance(token, instanceName).isPresent();
        } catch (Exception e) {
            log.warn("Error checking schema existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Finds a service instance by name.
     */
    private Optional<String> findServiceInstance(String token, String instanceName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<ServiceInstancesListResponse> response = restTemplate.exchange(
                credentials.getSmUrl() + "/v1/service_instances?fieldQuery=name eq '" + instanceName + "'",
                HttpMethod.GET,
                request,
                ServiceInstancesListResponse.class
            );
            
            if (response.getBody() != null && response.getBody().getItems() != null) {
                return response.getBody().getItems().stream()
                    .filter(item -> instanceName.equals(item.getName()))
                    .findFirst()
                    .map(ServiceInstanceResponse::getId);
            }
        } catch (Exception e) {
            log.debug("Error finding service instance: {}", e.getMessage());
        }
        
        return Optional.empty();
    }

    /**
     * Gets the HANA database ID from VCAP_SERVICES.
     */
    private String getHanaDatabaseId() {
        String vcapServices = System.getenv("VCAP_SERVICES");
        if (vcapServices == null) {
            throw new IllegalStateException("VCAP_SERVICES not available");
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, List<Map<String, Object>>> services = mapper.readValue(vcapServices, 
                new com.fasterxml.jackson.core.type.TypeReference<>() {});
            
            List<Map<String, Object>> hanaServices = services.get("hana-cloud");
            if (hanaServices == null || hanaServices.isEmpty()) {
                throw new IllegalStateException("No hana-cloud service binding found");
            }
            
            Map<String, Object> hanaCredentials = (Map<String, Object>) hanaServices.get(0).get("credentials");
            return (String) hanaCredentials.get("database_id");
        } catch (Exception e) {
            log.error("Failed to get HANA database ID: {}", e.getMessage());
            throw new RuntimeException("Failed to get HANA database ID", e);
        }
    }

    /**
     * Ensures Service Manager credentials are loaded.
     */
    private void ensureCredentials() {
        if (credentials != null) {
            return;
        }
        
        String vcapServices = System.getenv("VCAP_SERVICES");
        if (vcapServices == null) {
            throw new IllegalStateException("VCAP_SERVICES not available");
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, List<Map<String, Object>>> services = mapper.readValue(vcapServices, 
                new com.fasterxml.jackson.core.type.TypeReference<>() {});
            
            List<Map<String, Object>> smServices = services.get("service-manager");
            if (smServices == null || smServices.isEmpty()) {
                throw new IllegalStateException("No service-manager binding found");
            }
            
            Map<String, Object> smCredentials = (Map<String, Object>) smServices.get(0).get("credentials");
            
            credentials = new ServiceManagerCredentials();
            credentials.setClientId((String) smCredentials.get("clientid"));
            credentials.setClientSecret((String) smCredentials.get("clientsecret"));
            credentials.setTokenUrl((String) smCredentials.get("url"));
            credentials.setSmUrl((String) smCredentials.get("sm_url"));
            
            log.info("Service Manager credentials loaded. SM URL: {}", credentials.getSmUrl());
        } catch (Exception e) {
            log.error("Failed to load Service Manager credentials: {}", e.getMessage());
            throw new RuntimeException("Failed to load Service Manager credentials", e);
        }
    }

    /**
     * Gets an access token from Service Manager.
     */
    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return accessToken;
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(credentials.getClientId(), credentials.getClientSecret());
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                credentials.getTokenUrl() + "/oauth/token",
                HttpMethod.POST,
                request,
                TokenResponse.class
            );
            
            if (response.getBody() != null) {
                accessToken = response.getBody().getAccessToken();
                tokenExpiry = System.currentTimeMillis() + (response.getBody().getExpiresIn() - 60) * 1000;
                return accessToken;
            }
        } catch (Exception e) {
            log.error("Failed to get Service Manager token: {}", e.getMessage());
        }
        
        throw new RuntimeException("Failed to get Service Manager access token");
    }

    @Data
    private static class ServiceManagerCredentials {
        private String clientId;
        private String clientSecret;
        private String tokenUrl;
        private String smUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private int expiresIn;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ServiceInstanceResponse {
        private String id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ServiceInstancesListResponse {
        private List<ServiceInstanceResponse> items;
    }
}