package com.lms.mentoring.multitenancy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controller handling SaaS Provisioning Service subscription callbacks.
 * Handles tenant onboarding (subscription) and offboarding (unsubscription).
 */
@Slf4j
@RestController
@RequestMapping("/callback/v1.0")
@Profile("cloud")
@Tag(name = "Tenant Provisioning", description = "SaaS Provisioning Service subscription callbacks")
public class TenantProvisioningController {

    @Value("${vcap.application.uris[0]:localhost}")
    private String applicationUri;

    @Value("${APPROUTER_URL:}")
    private String approuterBaseUrl;

    /**
     * Returns the list of dependencies required for tenant subscription.
     * Called by SaaS Provisioning Service before subscription.
     */
    @Operation(
        summary = "Get dependencies",
        description = "Returns list of service dependencies required for tenant subscription"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dependencies returned successfully")
    })
    @GetMapping("/dependencies")
    public ResponseEntity<List<Map<String, Object>>> getDependencies() {
        log.info("Dependencies requested by SaaS Provisioning Service");
        
        // Return XSUAA as a dependency
        List<Map<String, Object>> dependencies = List.of(
            Map.of(
                "xsappname", "learning-management-system"
            )
        );
        
        log.debug("Returning {} dependencies", dependencies.size());
        return ResponseEntity.ok(dependencies);
    }

    /**
     * Handles tenant subscription.
     * Called by SaaS Provisioning Service when a tenant subscribes to the application.
     * Returns the tenant-specific URL to access the application.
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
        
        // In a real implementation, you would:
        // 1. Clean up tenant-specific data
        // 2. Remove tenant-specific database schemas (if using schema-per-tenant)
        // 3. Revoke tenant-specific access
        
        log.info("Tenant {} unsubscribed successfully", tenantId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Builds the tenant-specific approuter URL.
     * SAP BTP expects the URL in format: https://{subdomain}.{approuter-host}
     * The subdomain from subscription payload is the tenant's subdomain.
     */
    private String buildTenantUrl(String subdomain) {
        // For SAP BTP multitenancy, return approuter URL with tenant subdomain
        // Format: https://{tenant-subdomain}.{approuter-host}
        String baseUrl = "https://" + subdomain + ".0658761dtrial-dev-lms-approuter.cfapps.us10-001.hana.ondemand.com";
        log.debug("Built tenant URL: {}", baseUrl);
        return baseUrl;
    }
}