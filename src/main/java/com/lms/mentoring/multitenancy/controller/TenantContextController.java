package com.lms.mentoring.multitenancy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller providing current tenant context information.
 * Extracts tenant info from the JWT token to show which tenant is making the request.
 * 
 * This is useful for testing multitenancy - different subscribers will have different
 * tenant IDs (zid) in their JWT tokens.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tenant-context")
@Profile("cloud")
@Tag(name = "Tenant Context", description = "Current tenant information from JWT token")
@SecurityRequirement(name = "bearerAuth")
public class TenantContextController {

    /**
     * Returns current tenant context extracted from the JWT token.
     * 
     * Key fields for multitenancy:
     * - tenantId (zid): The zone/tenant ID - different for each subscriber
     * - subdomain (ext_attr.zdn): The subscriber's subdomain
     * - subaccountId (ext_attr.subaccountid): The subscriber's subaccount ID
     */
    @Operation(
        summary = "Get current tenant context",
        description = "Returns tenant information extracted from the JWT token. " +
                "Use this to verify which tenant (subscriber) is making the request."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tenant context retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTenantContext(Authentication authentication) {
        log.info("Tenant context requested");
        
        Map<String, Object> context = new HashMap<>();
        
        if (authentication == null) {
            context.put("error", "No authentication found");
            return ResponseEntity.ok(context);
        }
        
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // Extract tenant information from JWT claims
            String tenantId = jwt.getClaimAsString("zid");
            String clientId = jwt.getClaimAsString("client_id");
            String issuer = jwt.getClaimAsString("iss");
            
            // Extended attributes contain subdomain and subaccount info
            Map<String, Object> extAttr = jwt.getClaimAsMap("ext_attr");
            String subdomain = extAttr != null ? (String) extAttr.get("zdn") : null;
            String subaccountId = extAttr != null ? (String) extAttr.get("subaccountid") : null;
            
            // Scopes/authorities
            List<String> scopes = jwt.getClaimAsStringList("scope");
            List<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            
            // Build response
            context.put("tenantId", tenantId);
            context.put("subdomain", subdomain);
            context.put("subaccountId", subaccountId);
            context.put("clientId", clientId);
            context.put("issuer", issuer);
            context.put("scopes", scopes);
            context.put("authorities", authorities);
            context.put("tokenType", "JWT");
            
            // Provider vs Subscriber indicator
            // Provider tenant has the same tenantId as in VCAP_SERVICES
            String providerTenantId = System.getenv("VCAP_APPLICATION") != null ? 
                    extractProviderTenantId() : null;
            boolean isProvider = tenantId != null && tenantId.equals(providerTenantId);
            context.put("isProvider", isProvider);
            context.put("isSubscriber", !isProvider);
            context.put("providerTenantId", providerTenantId);
            
            log.info("Tenant context - tenantId: {}, subdomain: {}, isProvider: {}", 
                    tenantId, subdomain, isProvider);
        } else {
            context.put("authenticationType", authentication.getClass().getSimpleName());
            context.put("principal", authentication.getPrincipal().toString());
            log.warn("Authentication principal is not a JWT: {}", authentication.getPrincipal().getClass());
        }
        
        return ResponseEntity.ok(context);
    }
    
    /**
     * Extracts the provider tenant ID from VCAP_SERVICES.
     * The provider tenant is the one that owns the XSUAA service instance.
     */
    private String extractProviderTenantId() {
        try {
            String vcapServices = System.getenv("VCAP_SERVICES");
            if (vcapServices != null) {
                // Simple extraction - look for tenantid in xsuaa credentials
                int tenantIdIndex = vcapServices.indexOf("\"tenantid\"");
                if (tenantIdIndex > 0) {
                    int startQuote = vcapServices.indexOf("\"", tenantIdIndex + 10) + 1;
                    int endQuote = vcapServices.indexOf("\"", startQuote);
                    if (startQuote > 0 && endQuote > startQuote) {
                        return vcapServices.substring(startQuote, endQuote);
                    }
                }
                // Fallback: look for identityzone which is same as tenantid
                int zoneIndex = vcapServices.indexOf("\"identityzone\"");
                if (zoneIndex > 0) {
                    int startQuote = vcapServices.indexOf("\"", zoneIndex + 14) + 1;
                    int endQuote = vcapServices.indexOf("\"", startQuote);
                    if (startQuote > 0 && endQuote > startQuote) {
                        return vcapServices.substring(startQuote, endQuote);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract provider tenant ID from VCAP_SERVICES", e);
        }
        return null;
    }
}