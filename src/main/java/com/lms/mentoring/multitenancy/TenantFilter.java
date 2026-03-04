package com.lms.mentoring.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

/**
 * Filter that extracts tenant ID from JWT token and sets TenantContext.
 * Must run after Spring Security authentication.
 */
@Slf4j
@Component
@Profile("cloud")
@Order(Ordered.LOWEST_PRECEDENCE - 10) // Run after security filters but before request handling
public class TenantFilter extends OncePerRequestFilter {

    /**
     * Provider tenant ID from XSUAA - requests with this tenant use default schema.
     * Automatically set from VCAP_SERVICES XSUAA binding.
     */
    @Value("${vcap.services.lms-xsuaa.credentials.zoneid:}")
    private String providerTenantId;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantId = extractTenantFromSecurityContext();
            
            if (tenantId != null) {
                TenantContext.setCurrentTenant(tenantId);
                log.debug("Tenant context set to: {} for path: {}", tenantId, request.getRequestURI());
            } else {
                log.debug("No tenant found, using default for path: {}", request.getRequestURI());
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clear the tenant context after request processing
            TenantContext.clear();
        }
    }

    /**
     * Extract tenant ID from the authenticated JWT token.
     * The tenant ID is in the 'zid' (zone ID) claim.
     */
    private String extractTenantFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            
            // 'zid' is the zone ID (tenant ID) in XSUAA tokens
            String zid = jwt.getClaimAsString("zid");
            
            if (zid != null) {
                // Check if this is the provider tenant
                // Provider tenant should use default schema (null tenant ID)
                if (isProviderTenant(jwt, zid)) {
                    log.debug("Provider tenant detected ({}), using default schema", zid);
                    return TenantContext.DEFAULT_TENANT;
                }
                
                return zid;
            }
        }
        
        return null;
    }

    /**
     * Determines if the request is from the provider tenant.
     * Provider tenant is identified by comparing with VCAP provider tenant ID (zoneid).
     */
    private boolean isProviderTenant(Jwt jwt, String zid) {
        // Simple check: if providerTenantId (from VCAP zoneid) matches zid, it's provider
        if (providerTenantId != null && !providerTenantId.isEmpty() && providerTenantId.equals(zid)) {
            log.debug("Provider tenant matched: {} == {}", providerTenantId, zid);
            return true;
        }
        
        log.debug("Tenant {} is NOT provider (provider={})", zid, providerTenantId);
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip tenant filtering for:
        // - Actuator endpoints (health checks, etc.)
        // - Callback endpoints (SaaS provisioning) - they handle tenant differently
        return path.startsWith("/actuator/") || 
               path.startsWith("/callback/") ||
               path.equals("/");
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("TenantFilter initialized with provider tenant ID: {}", providerTenantId);
    }
}