package com.lms.mentoring.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hibernate MultiTenantConnectionProvider implementation.
 * Provides connections with the correct schema set based on tenant.
 * 
 * For subscriber tenants, if the schema doesn't exist, it will be created
 * lazily on first access to support H2 in-memory database scenarios.
 */
@Slf4j
@Component
@Profile("cloud")
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;
    private final TenantSchemaService tenantSchemaService;
    private String defaultSchema;
    
    // Track schemas we've verified exist (to avoid repeated checks)
    private final Set<String> verifiedSchemas = ConcurrentHashMap.newKeySet();

    @Autowired
    public TenantConnectionProvider(DataSource dataSource, @Lazy TenantSchemaService tenantSchemaService) {
        this.dataSource = dataSource;
        this.tenantSchemaService = tenantSchemaService;
        // Get the default schema from a fresh connection
        try (Connection conn = dataSource.getConnection()) {
            this.defaultSchema = conn.getSchema();
            log.info("Default schema detected: {}", defaultSchema);
        } catch (SQLException e) {
            log.warn("Could not determine default schema: {}", e.getMessage());
            this.defaultSchema = null;
        }
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantId) throws SQLException {
        Connection connection = getAnyConnection();
        
        String schemaName = TenantContext.toSchemaName(tenantId);
        
        if (schemaName != null) {
            // Ensure schema exists before switching (lazy creation for H2)
            ensureSchemaExists(tenantId, schemaName, connection);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET SCHEMA \"" + schemaName + "\"");
                log.info("Switched to schema: {} for tenant: {}", schemaName, tenantId);
            } catch (SQLException e) {
                log.error("Failed to switch to schema {} for tenant {}: {}", 
                    schemaName, tenantId, e.getMessage());
                connection.close();
                throw e;
            }
        } else {
            // Use default schema for provider tenant
            if (defaultSchema != null) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET SCHEMA \"" + defaultSchema + "\"");
                }
            }
            log.info("Using default schema for tenant: {}", tenantId);
        }
        
        return connection;
    }
    
    /**
     * Ensures the schema exists for a tenant, creating it if necessary.
     * This is important for H2 in-memory databases where schemas don't persist.
     */
    private void ensureSchemaExists(String tenantId, String schemaName, Connection connection) throws SQLException {
        // Skip if already verified
        if (verifiedSchemas.contains(schemaName)) {
            return;
        }
        
        // Check if schema exists
        boolean exists = checkSchemaExists(schemaName, connection);
        
        if (!exists) {
            log.info("Schema {} does not exist for tenant {}. Creating lazily...", schemaName, tenantId);
            try {
                // Create the schema using TenantSchemaService
                tenantSchemaService.createTenantSchema(tenantId);
                log.info("Schema {} created successfully for tenant {}", schemaName, tenantId);
            } catch (Exception e) {
                log.error("Failed to create schema {} for tenant {}: {}", schemaName, tenantId, e.getMessage());
                throw new SQLException("Failed to create schema for tenant: " + tenantId, e);
            }
        }
        
        // Mark as verified
        verifiedSchemas.add(schemaName);
    }
    
    /**
     * Checks if a schema exists in the database.
     */
    private boolean checkSchemaExists(String schemaName, Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Try HANA syntax first (SYS.SCHEMAS)
            try {
                ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM SYS.SCHEMAS WHERE SCHEMA_NAME = '" + schemaName + "'"
                );
                boolean exists = rs.next() && rs.getInt(1) > 0;
                rs.close();
                return exists;
            } catch (SQLException e) {
                // Fall back to INFORMATION_SCHEMA for H2/other databases
                log.warn("HANA schema check failed, trying INFORMATION_SCHEMA: {}", e.getMessage());
                ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + schemaName + "'"
                );
                boolean exists = rs.next() && rs.getInt(1) > 0;
                rs.close();
                return exists;
            }
        }
    }

    @Override
    public void releaseConnection(String tenantId, Connection connection) throws SQLException {
        // Reset to default schema before returning to pool
        if (defaultSchema != null) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET SCHEMA \"" + defaultSchema + "\"");
            } catch (SQLException e) {
                log.warn("Failed to reset schema: {}", e.getMessage());
            }
        }
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}