package com.lms.mentoring.unit.multitenancy;

import com.lms.mentoring.multitenancy.TenantContext;
import com.lms.mentoring.multitenancy.TenantFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TenantFilter}.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

    private TenantFilter tenantFilter;
    
    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        tenantFilter = new TenantFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_WithSubscriberTenant_ShouldSetTenantContext() throws ServletException, IOException {
        // given
        String subscriberTenantId = "subscriber-tenant-123";
        String providerTenantId = "provider-tenant-456";
        ReflectionTestUtils.setField(tenantFilter, "providerTenantId", providerTenantId);
        
        Jwt jwt = createJwt(subscriberTenantId);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        request.setRequestURI("/api/v1/courses");
        
        AtomicReference<String> capturedTenant = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedTenant.set(TenantContext.getCurrentTenant());
            return null;
        }).when(filterChain).doFilter(request, response);

        // when
        tenantFilter.doFilter(request, response, filterChain);

        // then
        assertEquals(subscriberTenantId, capturedTenant.get());
        // Context should be cleared after request
        assertFalse(TenantContext.isSet());
    }

    @Test
    void doFilter_WithProviderTenant_ShouldSetDefaultTenant() throws ServletException, IOException {
        // given
        String providerTenantId = "provider-tenant-123";
        ReflectionTestUtils.setField(tenantFilter, "providerTenantId", providerTenantId);
        
        Jwt jwt = createJwt(providerTenantId);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        request.setRequestURI("/api/v1/courses");
        
        AtomicReference<String> capturedTenant = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedTenant.set(TenantContext.getCurrentTenant());
            return null;
        }).when(filterChain).doFilter(request, response);

        // when
        tenantFilter.doFilter(request, response, filterChain);

        // then
        assertEquals("PROVIDER", capturedTenant.get());
    }

    @Test
    void doFilter_WithNoAuthentication_ShouldUseDefaultTenant() throws ServletException, IOException {
        // given
        request.setRequestURI("/api/v1/courses");
        
        AtomicReference<String> capturedTenant = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedTenant.set(TenantContext.getCurrentTenant());
            return null;
        }).when(filterChain).doFilter(request, response);

        // when
        tenantFilter.doFilter(request, response, filterChain);

        // then
        assertEquals("PROVIDER", capturedTenant.get());
    }

    @Test
    void doFilter_ShouldClearContextAfterRequest() throws ServletException, IOException {
        // given
        String tenantId = "test-tenant";
        ReflectionTestUtils.setField(tenantFilter, "providerTenantId", "different-provider");
        
        Jwt jwt = createJwt(tenantId);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        request.setRequestURI("/api/v1/courses");

        // when
        tenantFilter.doFilter(request, response, filterChain);

        // then
        assertFalse(TenantContext.isSet());
    }

    @Test
    void doFilter_WhenExceptionThrown_ShouldStillClearContext() throws ServletException, IOException {
        // given
        String tenantId = "test-tenant";
        ReflectionTestUtils.setField(tenantFilter, "providerTenantId", "different-provider");
        
        Jwt jwt = createJwt(tenantId);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        request.setRequestURI("/api/v1/courses");
        
        doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        // when & then
        assertThrows(RuntimeException.class, () -> 
            tenantFilter.doFilter(request, response, filterChain));
        
        // Context should still be cleared
        assertFalse(TenantContext.isSet());
    }

    @Test
    void doFilter_WithActuatorPath_ShouldSkipFiltering() throws ServletException, IOException {
        // given - actuator path should be skipped
        request.setRequestURI("/actuator/health");

        // when
        tenantFilter.doFilter(request, response, filterChain);

        // then - filter chain should be called, no tenant context set
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_WithCallbackPath_ShouldSkipFiltering() throws ServletException, IOException {
        // given - callback path should be skipped
        request.setRequestURI("/callback/v1.0/tenants/123");

        // when
        tenantFilter.doFilter(request, response, filterChain);

        // then - filter chain should be called
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_WithRootPath_ShouldSkipFiltering() throws ServletException, IOException {
        // given - root path should be skipped
        request.setRequestURI("/");

        // when
        tenantFilter.doFilter(request, response, filterChain);

        // then - filter chain should be called
        verify(filterChain).doFilter(request, response);
    }

    private Jwt createJwt(String zoneId) {
        return new Jwt(
            "test-token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "RS256"),
            Map.of("zid", zoneId, "sub", "test-user")
        );
    }
}