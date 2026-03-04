package com.lms.mentoring.multitenancy;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Initializes the provider (default) schema on application startup.
 * Since Liquibase auto-run is disabled for multi-tenancy, we need to
 * manually run migrations for the provider tenant's schema.
 */
@Slf4j
@Component
@Profile("cloud")
@RequiredArgsConstructor
public class TenantSchemaInitializer {

    private final DataSource dataSource;
    
    private static final String CHANGELOG_PATH = "db/changelog/db.changelog-master.xml";

    /**
     * Runs Liquibase migrations for the provider (default) schema on startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeProviderSchema() {
        log.info("Initializing provider tenant schema (default schema)...");
        
        try (Connection connection = dataSource.getConnection()) {
            String defaultSchema = connection.getSchema();
            log.info("Running Liquibase migrations for provider schema: {}", defaultSchema);
            
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            
            if (defaultSchema != null) {
                database.setDefaultSchemaName(defaultSchema);
                database.setLiquibaseSchemaName(defaultSchema);
            }
            
            try (Liquibase liquibase = new Liquibase(
                    CHANGELOG_PATH,
                    new ClassLoaderResourceAccessor(),
                    database)) {
                
                liquibase.update("");
                log.info("Provider schema migrations completed successfully");
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize provider schema: {}", e.getMessage(), e);
            // Don't fail startup - the schema might already exist
        }
    }
}