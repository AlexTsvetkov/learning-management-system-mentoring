package com.lms.mentoring.multitenancy.controller;

import com.lms.mentoring.multitenancy.ServiceManagerSchemaService;
import com.lms.mentoring.multitenancy.TenantContext;
import com.lms.mentoring.multitenancy.TenantSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller handling SaaS Provisioning Service subscription callbacks.
 * Handles tenant onboarding (subscription) and offboarding (unsubscription).
 * 
 * Schema Management Strategy:
 * - Uses ServiceManagerSchemaService to create/delete HANA schemas via Service Manager API
 * - Uses TenantSchemaService to run Liquibase migrations after schema creation
 */
@Slf4j
@RestController
@RequestMapping("/callback/v1.0")
@Profile("cloud")
@RequiredArgsConstructor
@Tag(name = "Tenant Provisioning", description = "SaaS Provisioning Service subscription callbacks")
public class TenantProvisioningController {

    private final TenantSchemaService tenantSchemaService;
    private final ServiceManagerSchemaService serviceManagerSchemaService;

    @Value("${vcap.application.uris[0]:localhost}")
    private String applicationUri;

    @Value("${APPROUTER_URL:}")
    private String approuterBaseUrl;

    /**
     * Destination service xsappname for dependency declaration.
     * This is populated from VCAP_SERVICES when destination service is bound.
     */
    @Value("${vcap.services.lms-destination.credentials.xsappname:#{null}}")
    private String destinationXsappname;

    /**
     * Returns the list of dependencies required for tenant subscription.
     * Called by SaaS Provisioning Service before subscription.
     * 
     * For multitenancy, we declare the Destination service as a dependency so that
     * subscribers can configure their own destinations (e.g., SMTP credentials).
     * When a subscriber subscribes, their subaccount must have entitlement to these services.
     */
    @Operation(
        summary = "Get dependencies",
        description = "Returns list of service dependencies required for tenant subscription. " +
                "Currently returns Destination service xsappname for multitenant destination configuration."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dependencies returned successfully")
    })
    @GetMapping("/dependencies")
    public ResponseEntity<List<Map<String, Object>>> getDependencies(
            @RequestParam(value = "tenantId", required = false) String tenantId) {
        log.info("Dependencies requested by SaaS Provisioning Service for tenantId: {}", tenantId);
        
        List<Map<String, Object>> dependencies = new ArrayList<>();
        
        // Add Destination service dependency if xsappname is available
        // This enables subscribers to configure their own destinations
        if (destinationXsappname != null && !destinationXsappname.isBlank()) {
            Map<String, Object> destinationDependency = new HashMap<>();
            destinationDependency.put("xsappname", destinationXsappname);
            dependencies.add(destinationDependency);
            log.info("Adding Destination service dependency: {}", destinationXsappname);
        } else {
            log.debug("Destination service xsappname not available, returning empty dependencies");
        }
        
        log.info("Returning {} dependencies for tenant {}", dependencies.size(), tenantId);
        return ResponseEntity.ok(dependencies);
    }

    /**
     * Handles tenant subscription.
     * Called by SaaS Provisioning Service when a tenant subscribes to the application.
     * Returns the tenant-specific URL to access the application.
     *
     * Subscription Flow:
     * 1. SaaS Registry calls PUT /callback/v1.0/tenants/{tenantId}
     * 2. ServiceManagerSchemaService attempts Service Manager schema creation
     *    → Falls back to direct SQL if hana-free plan detected
     * 3. TenantSchemaService runs Liquibase migrations
     * 4. Returns tenant-specific URL
     */
    @Operation(
        summary = "Subscribe tenant",
        description = "Handles tenant subscription and returns tenant-specific approuter URL"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tenant subscribed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid subscription request")
    })
    @PutMapping("/tenants/{tenantId}")
    public ResponseEntity<String> onSubscribe(
            @Parameter(description = "Tenant ID (subaccount ID)") @PathVariable String tenantId,
            @RequestBody(required = false) Map<String, Object> subscriptionPayload) {
        
        log.info("Tenant subscription request received for tenantId: {}", tenantId);
        
        if (subscriptionPayload != null) {
            log.debug("Subscription payload: {}", subscriptionPayload);
        }
        
        // Extract subdomain from payload if available
        String subdomain = "tenant";
        if (subscriptionPayload != null && subscriptionPayload.containsKey("subscribedSubdomain")) {
            subdomain = (String) subscriptionPayload.get("subscribedSubdomain");
        }
        
        // Create tenant-specific database schema via Service Manager and run migrations
        String schemaName = TenantContext.toSchemaName(tenantId);
        log.info("Creating schema {} for tenant {} (subdomain: {})", schemaName, tenantId, subdomain);
        
        try {
            // Step 1: Create HANA schema via Service Manager API
            log.info("Creating HANA schema via Service Manager for tenant {}", tenantId);
            serviceManagerSchemaService.createTenantSchema(tenantId, subdomain);
            log.info("HANA schema created via Service Manager for tenant {}", tenantId);
            
            // Step 2: Run Liquibase migrations for the new schema
            log.info("Running Liquibase migrations for tenant {} schema {}", tenantId, schemaName);
            tenantSchemaService.createTenantSchema(tenantId);
            log.info("Liquibase migrations completed for tenant {}", tenantId);
            
            log.info("Schema {} created and migrated successfully for tenant {}", schemaName, tenantId);
        } catch (Exception e) {
            log.error("Failed to create schema for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Failed to create tenant schema: " + e.getMessage());
        }
        
        // Build tenant-specific approuter URL
        String tenantUrl = buildTenantUrl(subdomain);
        
        log.info("Tenant {} subscribed successfully. URL: {}", tenantId, tenantUrl);
        
        // Return the tenant-specific URL as plain text (required by SaaS Provisioning Service)
        return ResponseEntity.ok(tenantUrl);
    }

    /**
     * Handles tenant unsubscription.
     * Called by SaaS Provisioning Service when a tenant unsubscribes from the application.
     */
    @Operation(
        summary = "Unsubscribe tenant",
        description = "Handles tenant unsubscription and cleanup"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tenant unsubscribed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid unsubscription request")
    })
    @DeleteMapping("/tenants/{tenantId}")
    public ResponseEntity<Void> onUnsubscribe(
            @Parameter(description = "Tenant ID (subaccount ID)") @PathVariable String tenantId) {
        
        log.info("Tenant unsubscription request received for tenantId: {}", tenantId);
        
        // Drop tenant-specific database schema via Service Manager
        String schemaName = TenantContext.toSchemaName(tenantId);
        log.info("Dropping schema {} for tenant {}", schemaName, tenantId);
        
        try {
            // Delete HANA schema via Service Manager API
            serviceManagerSchemaService.deleteTenantSchema(tenantId);
            log.info("Schema {} dropped successfully via Service Manager for tenant {}", schemaName, tenantId);
        } catch (Exception e) {
            log.warn("Failed to drop schema for tenant {}: {}", tenantId, e.getMessage());
            // Don't fail unsubscription if schema drop fails
        }
        
        log.info("Tenant {} unsubscribed successfully", tenantId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Builds the tenant URL for subscription.
     * 
     * For SAP BTP Trial accounts: Each tenant gets a dedicated route mapped to the approuter.
     * Route format: https://{subdomain}.cfapps.{region}.hana.ondemand.com
     * 
     * Note: The route must be manually mapped using:
     * cf map-route lms-approuter cfapps.{region}.hana.ondemand.com --hostname {subdomain}
     */
    private String buildTenantUrl(String subdomain) {
        // Build tenant-specific URL with subdomain as hostname
        // Format: https://{subdomain}.cfapps.us10-001.hana.ondemand.com
        
        // Extract domain from approuter URL or use default
        String domain = "cfapps.us10-001.hana.ondemand.com";
        
        if (approuterBaseUrl != null && !approuterBaseUrl.isEmpty()) {
            // Extract domain from APPROUTER_URL (e.g., https://org-space-app.cfapps.region.hana.ondemand.com)
            String host = approuterBaseUrl.replace("https://", "").replace("http://", "");
            int firstDot = host.indexOf('.');
            if (firstDot > 0) {
                domain = host.substring(firstDot + 1);
            }
        }
        
        String tenantUrl = "https://" + subdomain + "." + domain;
        
        log.info("Built tenant URL: {} (requires route mapping: cf map-route lms-approuter {} --hostname {})", 
                tenantUrl, domain, subdomain);
        return tenantUrl;
    }
}