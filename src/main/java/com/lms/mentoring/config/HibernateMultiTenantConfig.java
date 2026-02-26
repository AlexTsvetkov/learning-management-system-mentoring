package com.lms.mentoring.config;

import com.lms.mentoring.multitenancy.TenantConnectionProvider;
import com.lms.mentoring.multitenancy.TenantIdentifierResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;

/**
 * Hibernate configuration for schema-based multi-tenancy.
 * Configures Hibernate to use SCHEMA multi-tenancy strategy.
 */
@Slf4j
@Configuration
@Profile("cloud")
@RequiredArgsConstructor
public class HibernateMultiTenantConfig {

    private final TenantConnectionProvider tenantConnectionProvider;
    private final TenantIdentifierResolver tenantIdentifierResolver;

    /**
     * Customizes Hibernate properties for multi-tenancy.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return (Map<String, Object> hibernateProperties) -> {
            log.info("Configuring Hibernate for SCHEMA-based multi-tenancy");
            
            // Enable SCHEMA-based multi-tenancy
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, tenantConnectionProvider);
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
            
            log.info("Hibernate multi-tenancy configured successfully");
        };
    }
}