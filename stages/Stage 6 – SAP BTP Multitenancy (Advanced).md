# Stage 6 – SAP BTP Multitenancy — Advanced

> **Estimated Duration:** 1-2 weeks  
> **Prerequisites:** Stage 5 (SAP BTP Multitenancy Basics)

## Overview

This stage builds upon Stage 5 by implementing **advanced multitenancy features** including:
- Schema-per-tenant data isolation using Hibernate Multi-tenancy
- Service Manager integration for HANA schema management
- Tenant-aware Destination Service lookups with provider fallback

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Request Flow                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  JWT Token                                                          │
│  (contains zid = tenant ID)                                         │
│       │                                                             │
│       ▼                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐ │
│  │ TenantFilter│───▶│TenantContext│───▶│ TenantConnectionProvider│ │
│  │ (extract)   │    │ (ThreadLocal)│    │ (SET SCHEMA tenant_xxx) │ │
│  └─────────────┘    └─────────────┘    └─────────────────────────┘ │
│                                                                     │
│  Database Schemas:                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │ PROVIDER │  │TENANT_A  │  │TENANT_B  │  │TENANT_C  │   ...     │
│  │ (default)│  │(schema)  │  │(schema)  │  │(schema)  │           │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Functional Requirements [FR]

* The system must identify the current tenant from the **JWT** `zid` claim and use this to select the appropriate database schema
* It must automatically provision a dedicated database schema upon tenant **subscription** and de-provision it on **unsubscription**
* It must use a **per-tenant schema** to store and retrieve data through Hibernate's `MultiTenantConnectionProvider`
* The application must adapt external API calls (like Destination service) by first checking the **subscriber's configuration** before falling back to the **provider's**
* Service Manager integration for dynamic HANA schema management in cloud environment

---

## Implementation Checklist

| Step | Component | Description | Files to Create/Modify |
|------|-----------|-------------|------------------------|
| 1 | TenantContext | Thread-local storage for tenant ID | `TenantContext.java` |
| 2 | TenantFilter | Extract tenant from JWT token | `TenantFilter.java` |
| 3 | TenantIdentifierResolver | Hibernate tenant resolution | `TenantIdentifierResolver.java` |
| 4 | TenantConnectionProvider | Schema switching for connections | `TenantConnectionProvider.java` |
| 5 | TenantSchemaService | SQL-based schema creation | `TenantSchemaService.java` |
| 6 | TenantSchemaInitializer | Provider schema on startup | `TenantSchemaInitializer.java` |
| 7 | ServiceManagerSchemaService | HANA via Service Manager API | `ServiceManagerSchemaService.java` |
| 8 | TenantAwareDestinationService | Fallback destination lookup | `TenantAwareDestinationService.java` |
| 9 | HibernateMultiTenantConfig | Hibernate configuration | `HibernateMultiTenantConfig.java` |
| 10 | Unit Tests | Test coverage for all services | `*Test.java` files |

---

## Package Structure

```
com.lms.mentoring.multitenancy/
├── TenantContext.java                    # Thread-local tenant storage
├── TenantFilter.java                     # JWT tenant extraction filter
├── TenantIdentifierResolver.java         # Hibernate tenant resolution
├── TenantConnectionProvider.java         # Hibernate schema switching
├── TenantSchemaService.java              # SQL schema creation/deletion
├── TenantSchemaInitializer.java          # Provider schema initialization
├── ServiceManagerSchemaService.java      # Service Manager HANA integration
├── controller/
│   └── TenantProvisioningController.java # SaaS subscription callbacks
└── dto/
    └── ApplicationInfoDto.java           # XSUAA credentials DTO

com.lms.mentoring.notification.smtp/
├── TenantAwareDestinationService.java    # Fallback destination lookup
├── DestinationSmtpCredentialsProvider.java # Updated for tenant awareness
└── ... (existing files)

com.lms.mentoring.config/
└── HibernateMultiTenantConfig.java       # Hibernate multi-tenant configuration
```

---

## Steps

### 1. [JAVA] Implement TenantContext (Thread-Local Storage)

Create a utility class for storing the current tenant ID in a thread-local variable.

**Purpose:** Provides request-scoped tenant isolation without passing tenant ID through all method parameters.

**TenantContext.java:**
```java
package com.lms.mentoring.multitenancy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {
    
    public static final String DEFAULT_TENANT = "PROVIDER";
    
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    public static String getCurrentTenant() {
        String tenant = currentTenant.get();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }
    
    public static void setCurrentTenant(String tenantId) {
        log.debug("Setting tenant context to: {}", tenantId);
        currentTenant.set(tenantId);
    }
    
    public static void clear() {
        log.debug("Clearing tenant context");
        currentTenant.remove();
    }
    
    public static boolean isSet() {
        return currentTenant.get() != null;
    }
    
    /**
     * Converts tenant ID (UUID) to HANA schema name.
     * Format: TENANT_<first 8 chars uppercase>
     * Example: "abc12345-xxxx-yyyy-zzzz" → "TENANT_ABC12345"
     */
    public static String toSchemaName(String tenantId) {
        if (tenantId == null || DEFAULT_TENANT.equals(tenantId)) {
            return DEFAULT_TENANT;
        }
        String prefix = tenantId.length() >= 8 
            ? tenantId.substring(0, 8) 
            : tenantId;
        return "TENANT_" + prefix.toUpperCase().replace("-", "");
    }
}
```

**Key Methods:**
| Method | Description |
|--------|-------------|
| `getCurrentTenant()` | Returns current tenant ID or "PROVIDER" if not set |
| `setCurrentTenant(String)` | Sets tenant ID for current thread |
| `clear()` | Removes tenant context (IMPORTANT: call in finally block) |
| `toSchemaName(String)` | Converts UUID to HANA-compatible schema name |

**⚠️ Important:** Always call `clear()` at the end of request processing to prevent memory leaks.

---

### 2. [JAVA] Implement TenantFilter (JWT Extraction)

Create a Spring Security filter that extracts tenant ID from the JWT token's `zid` claim.

**TenantFilter.java:**
```java
package com.lms.mentoring.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Profile("cloud")
public class TenantFilter extends OncePerRequestFilter {
    
    @Value("${vcap.services.lms-xsuaa.credentials.identityzone:#{null}}")
    private String providerTenantId;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            String tenantId = extractTenantFromSecurityContext();
            TenantContext.setCurrentTenant(tenantId);
            
            log.debug("Request to {} - Tenant: {}", request.getRequestURI(), tenantId);
            
            filterChain.doFilter(request, response);
        } finally {
            // CRITICAL: Always clear to prevent tenant leakage between requests
            TenantContext.clear();
        }
    }
    
    private String extractTenantFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String zid = jwt.getClaimAsString("zid");
            
            if (zid != null && !isProviderTenant(zid)) {
                return zid;
            }
        }
        
        return TenantContext.DEFAULT_TENANT;
    }
    
    private boolean isProviderTenant(String zid) {
        return providerTenantId != null && providerTenantId.equals(zid);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filtering for actuator, callbacks, and root
        return path.startsWith("/actuator/") || 
               path.startsWith("/callback/") ||
               path.equals("/");
    }
}
```

**Key Points:**
- Uses `OncePerRequestFilter` to ensure single execution per request
- Extracts `zid` claim from JWT token (XSUAA's tenant identifier)
- Compares against provider tenant ID to detect provider vs subscriber
- **Always clears context in finally block** to prevent memory leaks

---

### 3. [JAVA] Implement TenantIdentifierResolver (Hibernate)

Bridge between TenantContext and Hibernate's multi-tenancy support.

**TenantIdentifierResolver.java:**
```java
package com.lms.mentoring.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {
    
    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        log.trace("Resolving current tenant: {}", tenant);
        return tenant;
    }
    
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
```

---

### 4. [JAVA] Implement TenantConnectionProvider (Schema Switching)

Implements Hibernate's `MultiTenantConnectionProvider` to switch database schemas.

**TenantConnectionProvider.java:**
```java
package com.lms.mentoring.multitenancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {
    
    private final DataSource dataSource;
    
    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }
    
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        
        String schemaName = TenantContext.toSchemaName(tenantIdentifier);
        log.debug("Switching to schema: {} for tenant: {}", schemaName, tenantIdentifier);
        
        try {
            // SET SCHEMA works for both HANA and H2
            connection.createStatement().execute("SET SCHEMA " + schemaName);
        } catch (SQLException e) {
            log.error("Failed to set schema {} for tenant {}", schemaName, tenantIdentifier, e);
            throw e;
        }
        
        return connection;
    }
    
    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // Reset to default schema before returning to pool
            connection.createStatement().execute("SET SCHEMA " + TenantContext.DEFAULT_TENANT);
        } finally {
            connection.close();
        }
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
        throw new UnsupportedOperationException("Unwrap not supported");
    }
}
```

**⚠️ Important:** The `SET SCHEMA` command is used to switch the current schema context. This works for both SAP HANA and H2 (for testing).

---

### 5. [JAVA] Implement TenantSchemaService (SQL Schema Management)

Handles schema creation with Liquibase migrations.

**TenantSchemaService.java:**
```java
package com.lms.mentoring.multitenancy;

import liquibase.integration.spring.SpringLiquibase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSchemaService {
    
    private final DataSource dataSource;
    
    public void createTenantSchema(String tenantId) {
        String schemaName = TenantContext.toSchemaName(tenantId);
        log.info("Creating schema {} for tenant {}", schemaName, tenantId);
        
        try (Connection conn = dataSource.getConnection()) {
            // 1. Create schema if not exists
            if (!schemaExists(schemaName, conn)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE SCHEMA " + schemaName);
                    log.info("Schema {} created", schemaName);
                }
            }
            
            // 2. Run Liquibase migrations
            runLiquibaseMigrations(schemaName);
            
        } catch (Exception e) {
            log.error("Failed to create schema for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to create tenant schema", e);
        }
    }
    
    public void dropTenantSchema(String tenantId) {
        String schemaName = TenantContext.toSchemaName(tenantId);
        log.info("Dropping schema {} for tenant {}", schemaName, tenantId);
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA " + schemaName + " CASCADE");
            log.info("Schema {} dropped", schemaName);
        } catch (SQLException e) {
            log.error("Failed to drop schema for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to drop tenant schema", e);
        }
    }
    
    public boolean schemaExists(String tenantId) {
        String schemaName = TenantContext.toSchemaName(tenantId);
        try (Connection conn = dataSource.getConnection()) {
            return schemaExists(schemaName, conn);
        } catch (SQLException e) {
            log.warn("Error checking schema existence: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean schemaExists(String schemaName, Connection conn) throws SQLException {
        // Works for both HANA and H2
        String sql = "SELECT 1 FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" 
                     + schemaName.toUpperCase() + "'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }
    
    private void runLiquibaseMigrations(String schemaName) throws Exception {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.xml");
        liquibase.setDefaultSchema(schemaName);
        liquibase.setLiquibaseSchema(schemaName);
        liquibase.afterPropertiesSet();
        log.info("Liquibase migrations completed for schema {}", schemaName);
    }
}
```

---

### 6. [JAVA] Implement TenantSchemaInitializer (Provider Schema)

Initializes the provider (default) schema on application startup.

**TenantSchemaInitializer.java:**
```java
package com.lms.mentoring.multitenancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("cloud")
@RequiredArgsConstructor
public class TenantSchemaInitializer {
    
    private final TenantSchemaService tenantSchemaService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeProviderSchema() {
        log.info("Initializing provider schema: {}", TenantContext.DEFAULT_TENANT);
        
        try {
            if (!tenantSchemaService.schemaExists(TenantContext.DEFAULT_TENANT)) {
                tenantSchemaService.createTenantSchema(TenantContext.DEFAULT_TENANT);
            } else {
                log.info("Provider schema already exists");
            }
        } catch (Exception e) {
            log.error("Failed to initialize provider schema: {}", e.getMessage(), e);
        }
    }
}
```

---

### 7. [JAVA] Implement ServiceManagerSchemaService (HANA via API)

For SAP HANA, use Service Manager API for schema management.

**ServiceManagerSchemaService.java:**
```java
package com.lms.mentoring.multitenancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Profile("cloud")
@RequiredArgsConstructor
public class ServiceManagerSchemaService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Creates a HANA schema via Service Manager API.
     * Falls back to local ID if Service Manager is unavailable (hana-free plan).
     */
    public String createTenantSchema(String tenantId, String subdomain) {
        log.info("Creating HANA schema via Service Manager for tenant: {}", tenantId);
        
        try {
            ensureCredentials();
        } catch (Exception e) {
            log.warn("Service Manager not available, using direct SQL: {}", e.getMessage());
            return "local-" + tenantId;
        }
        
        // Try to get HANA database_id (not available in hana-free plan)
        String databaseId = getHanaDatabaseId();
        if (databaseId == null) {
            log.info("HANA database_id not available (hana-free plan). Using direct SQL.");
            return "direct-schema-" + tenantId.substring(0, Math.min(8, tenantId.length()));
        }
        
        // ... Service Manager API calls
        // (See full implementation in source code)
        
        return "instance-" + tenantId;
    }
    
    public void deleteTenantSchema(String tenantId) {
        // ... Implementation
    }
    
    public boolean schemaExists(String tenantId) {
        // ... Implementation
    }
    
    private void ensureCredentials() {
        // Load from VCAP_SERVICES environment variable
        String vcapServices = System.getenv("VCAP_SERVICES");
        if (vcapServices == null) {
            throw new IllegalStateException("VCAP_SERVICES not available");
        }
        // Parse service-manager credentials
    }
    
    private String getHanaDatabaseId() {
        // Extract database_id from hana-cloud service binding
        // Returns null for hana-free plan
    }
}
```

**Note:** The `hana-free` plan in BTP Trial doesn't provide `database_id`, so Service Manager schema creation won't work. The service gracefully falls back to direct SQL schema creation via `TenantSchemaService`.

---

### 8. [JAVA] Implement TenantAwareDestinationService (Fallback Lookup)

Implements the pattern: Try subscriber destination first, fallback to provider.

**TenantAwareDestinationService.java:**
```java
package com.lms.mentoring.notification.smtp;

import com.sap.cloud.sdk.cloudplatform.connectivity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@Profile("cloud")
public class TenantAwareDestinationService {
    
    @Value("${vcap.services.lms-xsuaa.credentials.identityzone:#{null}}")
    private String providerTenantId;
    
    /**
     * Retrieves a destination with fallback:
     * 1. Try subscriber tenant's destination
     * 2. Fallback to provider tenant's destination
     */
    public Optional<Destination> getDestinationWithFallback(String destinationName) {
        // 1. Try current tenant (subscriber)
        try {
            Optional<Destination> subscriberDestination = 
                DestinationAccessor.tryGetDestination(destinationName);
            
            if (subscriberDestination.isPresent()) {
                log.debug("Found destination '{}' in subscriber tenant", destinationName);
                return subscriberDestination;
            }
        } catch (Exception e) {
            log.debug("Destination '{}' not found in subscriber: {}", destinationName, e.getMessage());
        }
        
        // 2. Fallback to provider tenant
        try {
            if (providerTenantId != null) {
                log.info("Falling back to provider tenant for destination '{}'", destinationName);
                
                Destination providerDestination = DestinationAccessor
                    .getLoader()
                    .tryGetDestination(destinationName, 
                        DestinationOptions.builder()
                            .augmentBuilder(
                                DestinationServiceOptionsAugmenter.augmenter()
                                    .retrievalStrategy(
                                        DestinationServiceRetrievalStrategy.ALWAYS_PROVIDER))
                            .build())
                    .orElse(null);
                
                if (providerDestination != null) {
                    log.debug("Found destination '{}' in provider tenant", destinationName);
                    return Optional.of(providerDestination);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get destination from provider: {}", e.getMessage());
        }
        
        return Optional.empty();
    }
}
```

**Fallback Flow:**
```
┌────────────────┐     ┌─────────────────────┐     ┌────────────────┐
│ API Request    │────▶│  TenantAware        │────▶│ Destination    │
│ (Subscriber)   │     │  DestinationService │     │ Service        │
└────────────────┘     └─────────────────────┘     └────────────────┘
                              │                           │
                              ▼                           │
                       Try subscriber                     │
                       destination                        │
                              │                           │
                     ┌────────┴────────┐                 │
                     │                 │                 │
                   Found           Not Found             │
                     │                 │                 │
                     ▼                 ▼                 │
                  Return          Try provider  ◀────────┘
                                  destination
                                      │
                                      ▼
                                   Return
```

---

### 9. [JAVA] Configure HibernateMultiTenantConfig

Configure Hibernate for SCHEMA-based multi-tenancy.

**HibernateMultiTenantConfig.java:**
```java
package com.lms.mentoring.config;

import com.lms.mentoring.multitenancy.TenantConnectionProvider;
import com.lms.mentoring.multitenancy.TenantIdentifierResolver;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class HibernateMultiTenantConfig {
    
    private final TenantConnectionProvider tenantConnectionProvider;
    private final TenantIdentifierResolver tenantIdentifierResolver;
    
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            hibernateProperties.put(
                AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, 
                tenantConnectionProvider);
            hibernateProperties.put(
                AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, 
                tenantIdentifierResolver);
        };
    }
}
```

**application.yml (cloud profile):**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        multiTenancy: SCHEMA
```

---

### 10. [JAVA] Update TenantProvisioningController

Update the subscription callbacks to use schema services.

**Key Changes:**
```java
@PutMapping("/tenants/{tenantId}")
public ResponseEntity<String> onSubscription(
        @PathVariable String tenantId,
        @RequestBody Map<String, Object> payload) {
    
    String subdomain = (String) payload.get("subscribedSubdomain");
    
    // 1. Create schema via Service Manager (or direct SQL as fallback)
    try {
        serviceManagerSchemaService.createTenantSchema(tenantId, subdomain);
    } catch (Exception e) {
        log.warn("Service Manager failed, using direct SQL: {}", e.getMessage());
    }
    
    // 2. Create schema with Liquibase migrations
    tenantSchemaService.createTenantSchema(tenantId);
    
    // 3. Return tenant URL (MUST be plain text)
    String tenantUrl = buildTenantUrl(subdomain);
    return ResponseEntity.ok(tenantUrl);
}

@DeleteMapping("/tenants/{tenantId}")
public ResponseEntity<Void> onUnsubscription(@PathVariable String tenantId) {
    
    // Drop the tenant schema
    tenantSchemaService.dropTenantSchema(tenantId);
    
    return ResponseEntity.ok().build();
}
```

---

### 11. [CF] Update cf-services.sh

Add Service Manager service (required for schema management via API).

```bash
# Add Service Manager service
SERVICE_MANAGER_SERVICE_NAME="lms-service-manager"

# Step 8: Service Manager Service
echo "[8/9] Service Manager Service"
create_service_if_not_exists "service-manager" "container" "$SERVICE_MANAGER_SERVICE_NAME"
```

**Updated Service Count:** 9 services total (was 8)

---

### 12. [CF] Update mta.yaml

Add Service Manager binding to the backend application.

```yaml
# Backend Application requires
requires:
  - name: lms-service-manager  # NEW

# Service Manager Resource
resources:
  - name: lms-service-manager
    type: org.cloudfoundry.managed-service
    parameters:
      service: service-manager
      service-plan: container
      service-name: lms-service-manager
```

---

### 13. [TEST] Create Unit Tests

Create comprehensive unit tests for all new services.

**Test Files to Create:**
| Test File | Tests |
|-----------|-------|
| `TenantContextTest.java` | Thread-local behavior, schema name conversion |
| `TenantFilterTest.java` | JWT extraction, path filtering |
| `TenantIdentifierResolverTest.java` | Hibernate resolution |
| `TenantSchemaServiceTest.java` | Schema CRUD operations |
| `ServiceManagerSchemaServiceTest.java` | Fallback behavior, API calls |
| `TenantAwareDestinationServiceTest.java` | Provider fallback logic |
| `FeatureFlagsServiceTest.java` | Flag evaluation, defaults |
| `TenantProvisioningControllerTest.java` | Subscription callbacks |

**Run Tests:**
```bash
mvn test -Dtest="*TenantTest,*SchemaServiceTest,*DestinationServiceTest"
```

---

### 14. [CF] Post-Deployment Route Mapping

After deployment, map subscriber routes to the approuter.

**Create script: `scripts/post-deploy-routes.sh`**
```bash
#!/bin/bash
# Map subscriber route after deployment
SUBSCRIBER_SUBDOMAIN="${1:-lms-subscriber-pekroa1q}"
APP_NAME="lms-approuter"
DOMAIN="cfapps.us10-001.hana.ondemand.com"

cf map-route "${APP_NAME}" "${DOMAIN}" --hostname "${SUBSCRIBER_SUBDOMAIN}"
```

**Usage:**
```bash
# After cf deploy completes:
./scripts/post-deploy-routes.sh
```

---

## Verification Steps

### Test Tenant Subscription Flow

```bash
# 1. Deploy the application
mvn clean package -P cloud -DskipTests
mbt build
cf deploy mta_archives/learning-management-system_0.1.0.mtar

# 2. Map subscriber route
./scripts/post-deploy-routes.sh

# 3. Get provider OAuth token
export TOKEN=$(curl -X POST "$XSUAA_URL/oauth/token" \
  -u "$CLIENT_ID:$CLIENT_SECRET" \
  -d "grant_type=client_credentials" | jq -r '.access_token')

# 4. Test subscription callback (simulated)
curl -X PUT "https://backend-url/callback/v1.0/tenants/test-tenant-123" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"subscribedSubdomain": "test-tenant"}'

# Expected: Returns tenant URL

# 5. Verify schema was created (check logs)
cf logs learning-management-system --recent | grep -i schema

# 6. Access data via subscriber URL
curl "https://test-tenant.cfapps.us10-001.hana.ondemand.com/api/v1/students" \
  -H "Authorization: Bearer $SUBSCRIBER_TOKEN"
```

### Verification Checklist

- [ ] TenantContext correctly stores/retrieves tenant ID
- [ ] TenantFilter extracts `zid` from JWT token
- [ ] Schema is created on subscription
- [ ] Liquibase migrations run in tenant schema
- [ ] Data is isolated per tenant
- [ ] Schema is dropped on unsubscription
- [ ] Destination fallback works (subscriber → provider)
- [ ] All unit tests pass

---

## Common Issues and Solutions

### Schema Already Exists Error

**Symptom:** `CREATE SCHEMA` fails with "already exists"

**Solution:** Check `schemaExists()` before creating:
```java
if (!schemaExists(schemaName, conn)) {
    stmt.execute("CREATE SCHEMA " + schemaName);
}
```

### Service Manager 403 Forbidden

**Symptom:** Service Manager API returns 403

**Solution:** Ensure `service-manager` service is bound with `container` plan and token has correct scopes.

### Tenant Data Leaking Between Requests

**Symptom:** One tenant sees another tenant's data

**Solution:** Ensure `TenantContext.clear()` is called in `finally` block:
```java
try {
    TenantContext.setCurrentTenant(tenantId);
    filterChain.doFilter(request, response);
} finally {
    TenantContext.clear();  // CRITICAL!
}
```

### Destination Not Found in Subscriber

**Symptom:** Destination lookup fails for subscriber

**Solution:** 
1. Create destination with same name in subscriber subaccount
2. Or verify `TenantAwareDestinationService` fallback is working

---

## Achievements

### [CF]
| Concept | Description |
|---------|-------------|
| Service Manager API | Dynamic HANA schema management via REST API |
| Multitenancy on DB level | Schema-per-tenant data isolation |
| Destination Service multitenancy | Subscriber/provider fallback pattern |
| VCAP_SERVICES parsing | Extract service credentials from environment |

### [JAVA]
| Concept | Description |
|---------|-------------|
| ThreadLocal variables | Request-scoped storage without parameter passing |
| HTTP request filters | `OncePerRequestFilter` for tenant extraction |
| Request context | `TenantContext` for current tenant access |
| Hibernate MultiTenantConnectionProvider | Schema switching per request |
| Hibernate CurrentTenantIdentifierResolver | Tenant resolution for Hibernate |

### [DB]
| Concept | Description |
|---------|-------------|
| DB connection pool | DataSource with schema switching |
| Hibernate multitenancy | SCHEMA-based isolation strategy |
| Liquibase per-tenant | Programmatic migration execution |
| SET SCHEMA | SQL command for schema context switching |

---

## Useful Resources

- [Hibernate Multi-tenancy](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#multitenacy)
- [SAP Cloud SDK Destination Service](https://sap.github.io/cloud-sdk/docs/java/features/connectivity/destination-service)
- [Service Manager API](https://help.sap.com/docs/service-manager/sap-service-manager/using-service-manager-apis)
- [Liquibase with Spring Boot](https://docs.liquibase.com/tools-integrations/springboot/springboot.html)
- [SAP BTP Multitenancy Development](https://help.sap.com/docs/btp/sap-business-technology-platform/developing-multitenant-applications-in-the-cloud-foundry-environment)

---

## Next Steps (Stage 7)

In **Stage 7 – Microservices**, you will:
- Split the monolith into separate microservices
- Implement inter-service communication
- Configure service mesh and API gateway