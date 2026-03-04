package com.lms.mentoring.unit.multitenancy;

import com.lms.mentoring.multitenancy.TenantContext;
import com.lms.mentoring.multitenancy.TenantIdentifierResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TenantIdentifierResolver}.
 */
@Tag("unit")
class TenantIdentifierResolverTest {

    private TenantIdentifierResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TenantIdentifierResolver();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolveCurrentTenantIdentifier_WhenTenantSet_ShouldReturnSetTenant() {
        // given
        String expectedTenant = "tenant-123";
        TenantContext.setCurrentTenant(expectedTenant);

        // when
        String resolvedTenant = resolver.resolveCurrentTenantIdentifier();

        // then
        assertEquals(expectedTenant, resolvedTenant);
    }

    @Test
    void resolveCurrentTenantIdentifier_WhenNoTenantSet_ShouldReturnDefaultTenant() {
        // when
        String resolvedTenant = resolver.resolveCurrentTenantIdentifier();

        // then
        assertEquals("PROVIDER", resolvedTenant);
    }

    @Test
    void resolveCurrentTenantIdentifier_AfterContextCleared_ShouldReturnDefaultTenant() {
        // given
        TenantContext.setCurrentTenant("tenant-to-clear");
        TenantContext.clear();

        // when
        String resolvedTenant = resolver.resolveCurrentTenantIdentifier();

        // then
        assertEquals("PROVIDER", resolvedTenant);
    }

    @Test
    void validateExistingCurrentSessions_ShouldReturnTrue() {
        // when
        boolean result = resolver.validateExistingCurrentSessions();

        // then
        assertTrue(result);
    }
}