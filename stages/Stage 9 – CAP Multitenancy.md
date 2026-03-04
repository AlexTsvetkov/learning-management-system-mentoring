# Stage 9 – CAP Multitenancy

> **Estimated Duration:** 1-2 weeks  
> **Prerequisites:** Stage 8 (CAP Basics), Stage 5-6 (Multitenancy Concepts)  
> **Branch:** `main-cap`

## Overview

In this stage, you will add **multitenancy support** to your CAP Java application. CAP provides built-in support for multitenancy, making it significantly simpler to implement compared to pure Spring Boot applications.

Key CAP multitenancy features:
- **Automatic tenant isolation** at the database level (HDI containers per tenant)
- **Built-in SaaS subscription callbacks**
- **Tenant-aware dependency injection**
- **MTX (Multitenancy Extension) services**

## Functional Requirements [FR]

* Enable multitenancy in the CAP project configuration
* Configure SaaS Registry for tenant subscription management
* Update xs-security.json with `tenant-mode: shared`
* Implement subscription callbacks using CAP's MTX services
* Add an Approuter for tenant-specific URL routing
* Configure Service Manager for tenant database provisioning
* Test subscription/unsubscription flows

## Required Services (Updated)

| Service | Plan | Purpose |
|---------|------|---------|
| HANA Cloud | hdi-shared | Main database (managed via Service Manager) |
| XSUAA | broker | OAuth 2.0 with tenant-shared mode |
| SaaS Registry | application | Marketplace registration & subscription management |
| Service Manager | container | Dynamic HDI container provisioning per tenant |
| Application Logging | standard | Centralized logging |
| Application Autoscaler | standard | Auto-scaling |
| Destination Service | lite | External service connectivity |

---

## Steps

### 1. Enable Multitenancy in CAP Configuration

Update the project configuration to enable multitenancy.

**package.json (root):**
```json
{
  "cds": {
    "requires": {
      "multitenancy": true,
      "toggles": true,
      "extensibility": true,
      "[production]": {
        "db": {
          "kind": "hana"
        },
        "auth": {
          "kind": "xsuaa"
        }
      }
    }
  }
}
```

**srv/src/main/resources/application.yaml:**
```yaml
spring:
  application:
    name: lms-cap

cds:
  security:
    xsuaa:
      enabled: true
  multitenancy:
    enabled: true
    mtxs:
      enabled: true
  odata-v4:
    endpoint:
      path: /odata/v4

---
spring:
  config:
    activate:
      on-profile: cloud

cds:
  multitenancy:
    enabled: true
    mtxs:
      enabled: true
    security:
      subscriptionScope: $XSAPPNAME.mtcallback
```

### 2. Update xs-security.json for Multitenancy

Change tenant-mode from `dedicated` to `shared` and add callback scope.

**xs-security.json:**
```json
{
    "xsappname": "lms-cap",
    "tenant-mode": "shared",
    "description": "LMS CAP - Multitenant OAuth2 Security Configuration",
    "scopes": [
        {
            "name": "$XSAPPNAME.user",
            "description": "User access"
        },
        {
            "name": "$XSAPPNAME.admin",
            "description": "Administrator access"
        },
        {
            "name": "$XSAPPNAME.mtcallback",
            "description": "Subscription callback scope",
            "grant-as-authority-to-apps": [
                "$XSAPPNAME(application,sap-provisioning,tenant-onboarding)"
            ]
        },
        {
            "name": "$XSAPPNAME.mtdeployment",
            "description": "HDI deployment scope",
            "grant-as-authority-to-apps": [
                "$XSAPPNAME(broker,lms-cap)"
            ]
        }
    ],
    "role-templates": [
        {
            "name": "User",
            "description": "Standard user role",
            "scope-references": ["$XSAPPNAME.user"]
        },
        {
            "name": "Admin",
            "description": "Administrator role",
            "scope-references": ["$XSAPPNAME.user", "$XSAPPNAME.admin"]
        }
    ],
    "role-collections": [
        {
            "name": "LMS_CAP_User",
            "description": "LMS CAP Users",
            "role-template-references": ["$XSAPPNAME.User"]
        },
        {
            "name": "LMS_CAP_Admin",
            "description": "LMS CAP Administrators",
            "role-template-references": ["$XSAPPNAME.Admin"]
        }
    ],
    "authorities": [
        "$XSAPPNAME.mtcallback",
        "$XSAPPNAME.mtdeployment"
    ],
    "oauth2-configuration": {
        "redirect-uris": ["https://*.cfapps.us10-001.hana.ondemand.com/**"],
        "token-validity": 900
    }
}
```

### 3. Create Approuter for Tenant Routing

Create an approuter module for tenant-specific URL handling.

**approuter/package.json:**
```json
{
    "name": "lms-cap-approuter",
    "version": "1.0.0",
    "scripts": {
        "start": "node node_modules/@sap/approuter/approuter.js"
    },
    "dependencies": {
        "@sap/approuter": "^16"
    }
}
```

**approuter/xs-app.json:**
```json
{
    "welcomeFile": "/odata/v4/LearningManagementService/$metadata",
    "authenticationMethod": "route",
    "sessionTimeout": 30,
    "routes": [
        {
            "source": "^/odata/(.*)$",
            "target": "/odata/$1",
            "destination": "lms-cap-srv",
            "authenticationType": "xsuaa",
            "csrfProtection": true
        },
        {
            "source": "^/api/(.*)$",
            "target": "/api/$1",
            "destination": "lms-cap-srv",
            "authenticationType": "xsuaa"
        },
        {
            "source": "^/actuator/(.*)$",
            "target": "/actuator/$1",
            "destination": "lms-cap-srv",
            "authenticationType": "none"
        },
        {
            "source": "^/-/cds/(.*)$",
            "target": "/-/cds/$1",
            "destination": "lms-cap-srv",
            "authenticationType": "none"
        }
    ]
}
```

### 4. Update MTA for Multitenancy

**mta.yaml:**
```yaml
_schema-version: "3.2"
ID: lms-cap
version: 1.0.0
description: Learning Management System - CAP Java Multitenant

parameters:
  enable-parallel-deployments: true

modules:
  # Approuter - Entry point with tenant URL routing
  - name: lms-cap-approuter
    type: approuter.nodejs
    path: approuter
    parameters:
      memory: 256M
      disk-quota: 256M
    properties:
      TENANT_HOST_PATTERN: "^(.*)-lms-cap-approuter.cfapps.us10-001.hana.ondemand.com"
    provides:
      - name: approuter-url
        properties:
          url: ${default-url}
    requires:
      - name: lms-cap-xsuaa
      - name: srv-api
        group: destinations
        properties:
          name: lms-cap-srv
          url: ~{srv-url}
          forwardAuthToken: true

  # Java Backend Service
  - name: lms-cap-srv
    type: java
    path: srv
    parameters:
      memory: 1024M
      buildpack: sap_java_buildpack_jakarta
    properties:
      SPRING_PROFILES_ACTIVE: cloud
      CDS_MULTITENANCY_MTXS_ENABLED: true
    provides:
      - name: srv-api
        properties:
          srv-url: ${default-url}
    requires:
      - name: lms-cap-xsuaa
      - name: lms-cap-service-manager
      - name: lms-cap-saas-registry
      - name: lms-cap-logging
      - name: lms-cap-destination
      - name: lms-cap-autoscaler

  # Database Deployer (for provider tenant schema)
  - name: lms-cap-db-deployer
    type: hdb
    path: db
    parameters:
      buildpack: nodejs_buildpack
    requires:
      - name: lms-cap-service-manager
        properties:
          hdi-container-name: ${service-name}

resources:
  # XSUAA - MUST use 'broker' plan for multitenancy
  - name: lms-cap-xsuaa
    type: org.cloudfoundry.managed-service
    parameters:
      service: xsuaa
      service-plan: broker
      path: xs-security.json

  # Service Manager - Dynamic HDI container provisioning
  - name: lms-cap-service-manager
    type: org.cloudfoundry.managed-service
    parameters:
      service: service-manager
      service-plan: container
      service-name: lms-cap-service-manager

  # SaaS Registry - Marketplace & subscription management
  - name: lms-cap-saas-registry
    type: org.cloudfoundry.managed-service
    parameters:
      service: saas-registry
      service-plan: application
      config:
        xsappname: lms-cap
        appName: lms-cap
        displayName: "Learning Management System (CAP)"
        description: "Multitenant LMS built with CAP Java"
        category: "Education"
        appUrls:
          getDependencies: ~{srv-api/srv-url}/-/cds/saas-provisioning/dependencies
          onSubscription: ~{srv-api/srv-url}/-/cds/saas-provisioning/tenant/{tenantId}
          onSubscriptionAsync: true
          onUnSubscriptionAsync: true
          callbackTimeoutMillis: 300000
    requires:
      - name: srv-api

  # Application Logging
  - name: lms-cap-logging
    type: org.cloudfoundry.managed-service
    parameters:
      service: application-logs
      service-plan: standard

  # Destination Service
  - name: lms-cap-destination
    type: org.cloudfoundry.managed-service
    parameters:
      service: destination
      service-plan: lite

  # Application Autoscaler
  - name: lms-cap-autoscaler
    type: org.cloudfoundry.managed-service
    parameters:
      service: autoscaler
      service-plan: standard
```

### 5. CAP MTX Subscription Callbacks

CAP provides built-in subscription handling via the `@sap/cds-mtxs` package. The callbacks are automatically exposed at:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/-/cds/saas-provisioning/tenant/{tenantId}` | PUT | Subscription callback |
| `/-/cds/saas-provisioning/tenant/{tenantId}` | DELETE | Unsubscription callback |
| `/-/cds/saas-provisioning/dependencies` | GET | Service dependencies |

**Optional: Custom Subscription Handler (Java)**

If you need custom logic during subscription, create a handler:

**srv/src/main/java/com/lms/mentoring/subscription/TenantSubscriptionHandler.java:**
```java
package com.lms.mentoring.subscription;

import com.sap.cds.services.mt.TenantProviderService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ServiceName(TenantProviderService.DEFAULT_NAME)
public class TenantSubscriptionHandler implements EventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(TenantSubscriptionHandler.class);

    @On(event = TenantProviderService.EVENT_SUBSCRIBE)
    public void onSubscribe(TenantProviderService.SubscribeEventContext context) {
        String tenantId = context.getTenant();
        log.info("Tenant {} subscription started", tenantId);
        
        // Custom initialization logic (e.g., seed data)
        // The HDI container is automatically created by CAP MTX
        
        log.info("Tenant {} subscription completed", tenantId);
    }

    @On(event = TenantProviderService.EVENT_UNSUBSCRIBE)
    public void onUnsubscribe(TenantProviderService.UnsubscribeEventContext context) {
        String tenantId = context.getTenant();
        log.info("Tenant {} unsubscription started", tenantId);
        
        // Custom cleanup logic
        // The HDI container is automatically dropped by CAP MTX
        
        log.info("Tenant {} unsubscription completed", tenantId);
    }
}
```

### 6. Tenant-Aware Services

CAP automatically provides tenant-aware database connections. To access tenant information in your handlers:

```java
import com.sap.cds.services.request.RequestContext;
import com.sap.cds.services.runtime.CdsRuntime;

@Component
public class TenantAwareService {

    @Autowired
    private CdsRuntime runtime;

    public String getCurrentTenant() {
        return RequestContext.getCurrent(runtime)
            .map(ctx -> ctx.getUserInfo().getTenant())
            .orElse("provider");
    }
}
```

### 7. Local Development with Mock Tenants

For local testing with multiple tenants:

**srv/src/main/resources/application-local.yaml:**
```yaml
cds:
  multitenancy:
    enabled: true
    security:
      enabled: false  # Disable for local testing

# Mock users for local testing
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          enabled: false

# Local mock tenant configuration
---
cds.multitenancy.tenants:
  t1:
    databaseId: "local-tenant-1"
  t2:
    databaseId: "local-tenant-2"
```

### 8. Build and Deploy

```bash
# Install dependencies
npm install

# Build Java service
cd srv && mvn clean install && cd ..

# Build MTA archive
mbt build

# Deploy
cf deploy mta_archives/lms-cap_1.0.0.mtar

# Map tenant routes (for BTP Trial)
cf map-route lms-cap-approuter cfapps.us10-001.hana.ondemand.com --hostname tenant-a
```

### 9. Create Postman Collections

**CAP Postman Collection Structure:**

```
LMS CAP API/
├── Auth/
│   ├── Get OAuth Token (Provider)
│   └── Get OAuth Token (Subscriber)
├── Students/
│   ├── List Students
│   ├── Get Student by ID
│   ├── Create Student
│   ├── Update Student
│   └── Delete Student
├── Courses/
│   ├── List Courses
│   ├── Get Course with Lessons
│   ├── Create Course
│   ├── Update Course
│   └── Delete Course
├── Enrollments/
│   ├── List Enrollments
│   ├── Enroll Student (Custom Action)
│   └── Purchase Course (Custom Action)
└── Admin/
    └── List All Students (Admin Only)
```

**Local Environment Variables:**
```json
{
    "baseUrl": "http://localhost:8080",
    "odataPath": "/odata/v4/LearningManagementService"
}
```

**Cloud Environment Variables:**
```json
{
    "approuterUrl": "https://<your-org>-lms-cap-approuter.cfapps.us10-001.hana.ondemand.com",
    "srvUrl": "https://<your-org>-lms-cap-srv.cfapps.us10-001.hana.ondemand.com",
    "odataPath": "/odata/v4/LearningManagementService",
    "tokenUrl": "{{xsuaaUrl}}/oauth/token",
    "clientId": "from-xsuaa-credentials",
    "clientSecret": "from-xsuaa-credentials"
}
```

---

## CAP vs Manual Multitenancy Comparison

| Aspect | CAP (Stage 9) | Manual (Stage 5-6) |
|--------|--------------|-------------------|
| Schema Creation | Automatic via MTX | Manual TenantSchemaService |
| Subscription Callbacks | Built-in (`/-/cds/saas-provisioning/`) | Custom TenantProvisioningController |
| Tenant Resolution | Automatic from JWT | Manual TenantFilter + TenantContext |
| Database Switching | Automatic | TenantConnectionProvider |
| Liquibase Migrations | HDI artifacts (.hdbtable, etc.) | Liquibase XML changelogs |
| Configuration | `package.json` + `cds` section | Multiple Java classes |

---

## Achievements

### [CAP]
| Concept | Description |
|---------|-------------|
| CAP MTX | Multitenancy Extension services for CAP |
| Automatic Tenant Isolation | Built-in HDI container management per tenant |
| `@sap/cds-mtxs` | Node.js package for subscription handling |
| TenantProviderService | Java event handler for subscription events |
| RequestContext | Access to current tenant in handlers |

### [SAP BTP]
| Concept | Description |
|---------|-------------|
| Service Manager (container plan) | Dynamic HDI container provisioning |
| XSUAA (broker plan) | Multitenant OAuth 2.0 |
| SaaS Registry (application plan) | Marketplace and subscription management |
| Async Subscription | Long-running subscription with callbacks |

---

## Useful Resources

- [CAP Multitenancy Documentation](https://cap.cloud.sap/docs/guides/multitenancy/)
- [CAP MTX Services](https://cap.cloud.sap/docs/guides/multitenancy/mtxs)
- [CAP Java MTX](https://cap.cloud.sap/docs/java/multitenancy)
- [SaaS Application Tutorial (CAP)](https://developers.sap.com/tutorials/cp-cf-security-xsuaa-multi-tenant.html)
- [HDI Container Concepts](https://help.sap.com/docs/HANA_CLOUD_DATABASE/c2b99f19e9264c4d9ae9221b22f6f589/14e3d42ad41c4d238d49fda67f69e62b.html)

---

## Verification Checklist

- [ ] Approuter accessible at provider URL
- [ ] Provider tenant can access OData services
- [ ] Application visible in BTP Marketplace
- [ ] Subscriber can subscribe successfully
- [ ] Subscriber tenant gets isolated HDI container
- [ ] Subscriber can access via tenant-specific URL
- [ ] Unsubscription cleans up HDI container
- [ ] Postman collection works for both environments