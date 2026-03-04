package com.lms.mentoring.multitenancy;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service for managing tenant database schemas.
 * Handles schema creation, deletion, and Liquibase migrations per tenant.
 */
@Slf4j
@Service
@Profile("cloud")
@RequiredArgsConstructor
public class TenantSchemaService {

    private final DataSource dataSource;
    
    private static final String CHANGELOG_PATH = "db/changelog/db.changelog-master.xml";

    /**
     * Creates a new schema for a tenant and runs Liquibase migrations.
     * 
     * @param tenantId the tenant ID
     * @throws RuntimeException if schema creation or migration fails
     */
    public void createTenantSchema(String tenantId) {
        String schemaName = TenantContext.toSchemaName(tenantId);
        
        if (schemaName == null) {
            log.info("Using default schema for provider tenant");
            return;
        }
        
        log.info("Creating schema {} for tenant {}", schemaName, tenantId);
        
        try (Connection connection = dataSource.getConnection()) {
            // Create schema
            createSchema(connection, schemaName);
            
            // Run Liquibase migrations for this schema
            runLiquibaseMigrations(connection, schemaName);
            
            log.info("Schema {} created and migrated successfully for tenant {}", schemaName, tenantId);
            
        } catch (Exception e) {
            log.error("Failed to create schema for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to create tenant schema: " + e.getMessage(), e);
        }
    }

    /**
     * Drops the schema for a tenant (used during unsubscription).
     * 
     * @param tenantId the tenant ID
     */
    public void dropTenantSchema(String tenantId) {
        String schemaName = TenantContext.toSchemaName(tenantId);
        
        if (schemaName == null) {
            log.info("Cannot drop default schema for provider tenant");
            return;
        }
        
        log.info("Dropping schema {} for tenant {}", schemaName, tenantId);
        
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            
            // CASCADE drops all objects in the schema
            stmt.execute("DROP SCHEMA \"" + schemaName + "\" CASCADE");
            
            log.info("Schema {} dropped successfully for tenant {}", schemaName, tenantId);
            
        } catch (SQLException e) {
            // Log but don't fail - schema might not exist
            log.warn("Failed to drop schema {} for tenant {}: {}", schemaName, tenantId, e.getMessage());
        }
    }

    /**
     * Checks if a schema exists for a tenant.
     * Works with both HANA (SYS.SCHEMAS) and H2 (INFORMATION_SCHEMA.SCHEMATA).
     * 
     * @param tenantId the tenant ID
     * @return true if schema exists
     */
    public boolean schemaExists(String tenantId) {
        String schemaName = TenantContext.toSchemaName(tenantId);
        
        if (schemaName == null) {
            return true; // Default schema always exists
        }
        
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            
            // Try HANA syntax first (SYS.SCHEMAS)
            String query = "SELECT COUNT(*) FROM SYS.SCHEMAS WHERE SCHEMA_NAME = '" + schemaName + "'";
            try {
                var rs = stmt.executeQuery(query);
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                // Fall back to standard INFORMATION_SCHEMA for H2/other databases
                log.debug("HANA schema check failed, trying INFORMATION_SCHEMA: {}", e.getMessage());
                var rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + schemaName + "'"
                );
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            log.warn("Failed to check schema existence for tenant {}: {}", tenantId, e.getMessage());
        }
        
        return false;
    }

    /**
     * Creates a schema if it doesn't exist.
     * Works with both HANA (SYS.SCHEMAS) and H2 (INFORMATION_SCHEMA.SCHEMATA).
     */
    private void createSchema(Connection connection, String schemaName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Check if schema exists - try HANA syntax first
            boolean exists = false;
            try {
                var rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM SYS.SCHEMAS WHERE SCHEMA_NAME = '" + schemaName + "'"
                );
                exists = rs.next() && rs.getInt(1) > 0;
                rs.close();
            } catch (SQLException e) {
                // Fall back to INFORMATION_SCHEMA for H2/other databases
                log.debug("HANA schema check failed, trying INFORMATION_SCHEMA: {}", e.getMessage());
                var rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + schemaName + "'"
                );
                exists = rs.next() && rs.getInt(1) > 0;
                rs.close();
            }
            
            if (!exists) {
                stmt.execute("CREATE SCHEMA \"" + schemaName + "\"");
                log.info("Created schema: {}", schemaName);
            } else {
                log.info("Schema {} already exists", schemaName);
            }
        }
    }

    /**
     * Runs Liquibase migrations for a specific schema.
     */
    private void runLiquibaseMigrations(Connection connection, String schemaName) throws Exception {
        log.info("Running Liquibase migrations for schema: {}", schemaName);
        
        // Set the schema for this connection
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET SCHEMA \"" + schemaName + "\"");
        }
        
        Database database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        database.setDefaultSchemaName(schemaName);
        database.setLiquibaseSchemaName(schemaName);
        
        try (Liquibase liquibase = new Liquibase(
                CHANGELOG_PATH,
                new ClassLoaderResourceAccessor(),
                database)) {
            
            liquibase.update("");
            log.info("Liquibase migrations completed for schema: {}", schemaName);
        }
    }
}