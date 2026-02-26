package com.lms.mentoring.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Hibernate MultiTenantConnectionProvider implementation.
 * Provides connections with the correct schema set based on tenant.
 */
@Slf4j
@Component
@Profile("cloud")
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;
    private String defaultSchema;

    @Autowired
    public TenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
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
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET SCHEMA \"" + schemaName + "\"");
                log.debug("Switched to schema: {} for tenant: {}", schemaName, tenantId);
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
            log.debug("Using default schema for tenant: {}", tenantId);
        }
        
        return connection;
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