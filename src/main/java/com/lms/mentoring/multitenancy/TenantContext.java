package com.lms.mentoring.multitenancy;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for current tenant context.
 * Each request thread has its own tenant ID.
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();

    /**
     * Default schema used for provider tenant and when no tenant is set.
     * In HANA, this is typically the schema from the service binding.
     */
    public static final String DEFAULT_TENANT = "PROVIDER";

    private TenantContext() {
        // Utility class
    }

    /**
     * Get the current tenant ID.
     * @return current tenant ID or DEFAULT_TENANT if not set
     */
    public static String getCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }

    /**
     * Set the current tenant ID.
     * @param tenantId the tenant ID to set
     */
    public static void setCurrentTenant(String tenantId) {
        log.debug("Setting current tenant to: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Clear the current tenant (call this at the end of request processing).
     */
    public static void clear() {
        log.debug("Clearing tenant context");
        CURRENT_TENANT.remove();
    }

    /**
     * Check if a tenant is currently set.
     * @return true if a tenant is set (other than default)
     */
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Convert tenant ID to HANA schema name.
     * Schema names must be uppercase and use underscores.
     * @param tenantId the tenant ID (UUID)
     * @return schema name for this tenant
     */
    public static String toSchemaName(String tenantId) {
        if (tenantId == null || DEFAULT_TENANT.equals(tenantId)) {
            return null; // Use default schema from connection
        }
        // Convert UUID to valid HANA schema name
        // Format: TENANT_<first 8 chars of UUID>
        String shortId = tenantId.replace("-", "").substring(0, 8).toUpperCase();
        return "TENANT_" + shortId;
    }
}