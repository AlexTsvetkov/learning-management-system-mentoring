package com.lms.mentoring.unit.multitenancy;

import com.lms.mentoring.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TenantContext}.
 */
@Tag("unit")
class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getCurrentTenant_WhenNotSet_ShouldReturnDefaultTenant() {
        // when
        String tenant = TenantContext.getCurrentTenant();

        // then
        assertEquals("PROVIDER", tenant);
    }

    @Test
    void getCurrentTenant_WhenSet_ShouldReturnSetValue() {
        // given
        String expectedTenant = "tenant-123";
        TenantContext.setCurrentTenant(expectedTenant);

        // when
        String tenant = TenantContext.getCurrentTenant();

        // then
        assertEquals(expectedTenant, tenant);
    }

    @Test
    void setCurrentTenant_ShouldSetValueInThreadLocal() {
        // given
        String tenantId = "test-tenant-456";

        // when
        TenantContext.setCurrentTenant(tenantId);

        // then
        assertEquals(tenantId, TenantContext.getCurrentTenant());
    }

    @Test
    void clear_ShouldRemoveCurrentTenant() {
        // given
        TenantContext.setCurrentTenant("tenant-to-clear");

        // when
        TenantContext.clear();

        // then
        assertEquals("PROVIDER", TenantContext.getCurrentTenant());
    }

    @Test
    void isSet_WhenNotSet_ShouldReturnFalse() {
        // when
        boolean result = TenantContext.isSet();

        // then
        assertFalse(result);
    }

    @Test
    void isSet_WhenSet_ShouldReturnTrue() {
        // given
        TenantContext.setCurrentTenant("some-tenant");

        // when
        boolean result = TenantContext.isSet();

        // then
        assertTrue(result);
    }

    @Test
    void isSet_AfterClear_ShouldReturnFalse() {
        // given
        TenantContext.setCurrentTenant("tenant-to-clear");
        TenantContext.clear();

        // when
        boolean result = TenantContext.isSet();

        // then
        assertFalse(result);
    }

    @Test
    void toSchemaName_WithProviderTenant_ShouldReturnNull() {
        // when
        String schemaName = TenantContext.toSchemaName("PROVIDER");

        // then
        assertNull(schemaName);
    }

    @Test
    void toSchemaName_WithTenantUuid_ShouldReturnFormattedSchemaName() {
        // given
        String tenantUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

        // when
        String schemaName = TenantContext.toSchemaName(tenantUuid);

        // then
        assertEquals("TENANT_A1B2C3D4", schemaName);
    }

    @Test
    void toSchemaName_WithExactly8CharTenantId_ShouldReturnFormattedSchemaName() {
        // given
        String tenantId = "abc12345";

        // when
        String schemaName = TenantContext.toSchemaName(tenantId);

        // then
        assertEquals("TENANT_ABC12345", schemaName);
    }

    @Test
    void toSchemaName_WithLowercaseTenantId_ShouldReturnUppercaseSchemaName() {
        // given
        String tenantId = "abcdefgh-1234";

        // when
        String schemaName = TenantContext.toSchemaName(tenantId);

        // then
        assertEquals("TENANT_ABCDEFGH", schemaName);
    }

    @Test
    void toSchemaName_WithNullTenantId_ShouldReturnNull() {
        // when
        String schemaName = TenantContext.toSchemaName(null);

        // then
        assertNull(schemaName);
    }

    @Test
    void toSchemaName_WithDefaultTenant_ShouldReturnNull() {
        // when
        String schemaName = TenantContext.toSchemaName(TenantContext.DEFAULT_TENANT);

        // then
        assertNull(schemaName);
    }
}