# Stage 5 – SAP BTP Multitenancy (Basics)

> **Estimated Duration:** 1-2 weeks  
> **Prerequisites:** Stage 2 (SAP BTP Basics), Stage 3 (Security with XSUAA)

## Overview

**Multitenancy** is a software architecture pattern where a single instance of an application serves multiple customers (tenants). Each tenant's data is isolated and invisible to other tenants, while sharing the same application infrastructure.

```
┌─────────────────────────────────────────────────────────────────┐
│                     SAP BTP Cloud Foundry                       │
├─────────────────────────────────────────────────────────────────┤
│  Provider Subaccount                                            │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────────────┐│
│  │ Approuter   │  │ Java App    │  │ Services                 ││
│  │ (entry pt)  │──│ (backend)   │──│ • XSUAA (broker plan)   ││
│  │             │  │             │  │ • SaaS Registry         ││
│  └─────────────┘  └─────────────┘  │ • HANA Cloud            ││
│                                     └──────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│  Subscriber Subaccounts (Tenants)                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ Tenant A    │  │ Tenant B    │  │ Tenant C    │             │
│  │ tenant-a.   │  │ tenant-b.   │  │ tenant-c.   │   ...       │
│  │ approuter   │  │ approuter   │  │ approuter   │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

---

## Functional Requirements [FR]

* The system must be configured with an **Approuter** to handle all incoming requests and provide a centralized entry point
* It must expose a secure endpoint (`/api/v1/application-info`), accessible only to users with the **ADMIN** role, that returns the OAuth token URL, client ID, and client secret of the bound XSUAA service
* The application must handle **subscription/unsubscription** callbacks from the SaaS Provisioning Service, dynamically returning a tenant-specific Approuter URL upon subscription
* All security **roles and scopes** must be defined in the `xs-security.json` file with `tenant-mode: shared`
* The application must be registered in a **marketplace** using the SaaS Provisioning Service to enable subscription by external tenant subaccounts

---

## Key Concepts

### Provider vs Subscriber Subaccounts

| Aspect | Provider Subaccount | Subscriber Subaccount |
|--------|--------------------|-----------------------|
| Purpose | Hosts and deploys the SaaS application | Subscribes to and uses the application |
| Deployment | Contains all modules (approuter, backend, services) | No deployment needed |
| Access | Via direct URL or BTP Cockpit | Via tenant-specific URL after subscription |
| Data | Provider's own data (if any) | Isolated tenant data |

### Approuter

The **SAP Application Router** is a Node.js application that:
- Acts as the single entry point for all requests
- Handles OAuth2 authentication via XSUAA
- Routes requests to backend services
- Provides tenant-specific URLs for SaaS applications

### SaaS Provisioning Service

The **SaaS Registry** service:
- Registers your application in the SAP BTP marketplace
- Manages tenant subscriptions and unsubscriptions
- Calls your application's callback endpoints during subscription lifecycle

---

## Package Structure (Stage 5 - Basics)

Stage 5 focuses on the **infrastructure** for multitenancy. The Java classes for tenant isolation (TenantContext, TenantFilter, etc.) are implemented in **Stage 6**.

```
com.lms.mentoring.multitenancy/
├── controller/
│   ├── ApplicationInfoController.java   # XSUAA credentials (Admin) [Stage 5]
│   ├── TenantContextController.java     # Current tenant info [Stage 5]
│   └── TenantProvisioningController.java # SaaS callbacks [Stage 5]
└── dto/
    └── ApplicationInfoDto.java          # XSUAA credentials DTO [Stage 5]

# The following are implemented in Stage 6:
# ├── TenantContext.java
# ├── TenantFilter.java
# ├── TenantConnectionProvider.java
# ├── TenantIdentifierResolver.java
# ├── TenantSchemaService.java
# └── TenantSchemaInitializer.java
```

### Controllers (Stage 5)

#### ApplicationInfoController

Admin-only endpoint exposing XSUAA credentials.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/application-info` | GET | Returns XSUAA credentials (tokenUrl, clientId, clientSecret, xsappname, identityZone) |

```java
@RestController
@RequestMapping("/api/v1")
@Profile("cloud")
public class ApplicationInfoController {
    
    @Value("${vcap.services.lms-xsuaa.credentials.url:}")
    private String xsuaaUrl;
    
    @Value("${vcap.services.lms-xsuaa.credentials.clientid:}")
    private String clientId;
    
    @Value("${vcap.services.lms-xsuaa.credentials.clientsecret:}")
    private String clientSecret;
    
    @GetMapping("/application-info")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ApplicationInfoDto getApplicationInfo() {
        return new ApplicationInfoDto(xsuaaUrl + "/oauth/token", clientId, clientSecret, ...);
    }
}
```

#### TenantProvisioningController

Handles SaaS subscription callbacks. **Note:** Schema creation is implemented in Stage 6.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/callback/v1.0/dependencies` | GET | Returns empty array (no dependencies) |
| `/callback/v1.0/tenants/{tenantId}` | PUT | Subscription: returns tenant URL |
| `/callback/v1.0/tenants/{tenantId}` | DELETE | Unsubscription: logs the event |

```java
@RestController
@RequestMapping("/callback/v1.0")
@Profile("cloud")
public class TenantProvisioningController {
    
    @Value("${APPROUTER_URL:}")
    private String approuterUrl;
    
    @GetMapping("/dependencies")
    public List<Object> getDependencies() {
        return Collections.emptyList();
    }
    
    @PutMapping("/tenants/{tenantId}")
    public ResponseEntity<String> onSubscription(
            @PathVariable String tenantId,
            @RequestBody Map<String, Object> payload) {
        
        String subdomain = (String) payload.get("subscribedSubdomain");
        log.info("Tenant {} subscribed with subdomain: {}", tenantId, subdomain);
        
        // Stage 5: Just return tenant URL
        // Stage 6: Will add schema creation
        String tenantUrl = buildTenantUrl(subdomain);
        return ResponseEntity.ok(tenantUrl);
    }
    
    @DeleteMapping("/tenants/{tenantId}")
    public ResponseEntity<Void> onUnsubscription(@PathVariable String tenantId) {
        log.info("Tenant {} unsubscribed", tenantId);
        
        // Stage 5: Just log
        // Stage 6: Will add schema deletion
        return ResponseEntity.ok().build();
    }
}
```

---

## Steps

### 1. [CF] Create and Configure Approuter

Create the Approuter module as the entry point for all requests.

**Directory Structure:**
```
approuter/
├── package.json
└── xs-app.json
```

**approuter/package.json:**
```json
{
  "name": "lms-approuter",
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
  "welcomeFile": "/api/v1/application-info",
  "authenticationMethod": "route",
  "sessionTimeout": 30,
  "routes": [
    {
      "source": "^/api/(.*)$",
      "target": "/api/$1",
      "destination": "lms-backend",
      "authenticationType": "none",
      "csrfProtection": false
    },
    {
      "source": "^/actuator/(.*)$",
      "target": "/actuator/$1",
      "destination": "lms-backend",
      "authenticationType": "none"
    },
    {
      "source": "^/swagger-ui/(.*)$",
      "target": "/swagger-ui/$1",
      "destination": "lms-backend",
      "authenticationType": "none"
    },
    {
      "source": "^/v3/api-docs(.*)$",
      "target": "/v3/api-docs$1",
      "destination": "lms-backend",
      "authenticationType": "none"
    }
  ]
}
```

**Key Configuration Properties:**
| Property | Description |
|----------|-------------|
| `welcomeFile` | Default landing page when accessing the approuter root URL |
| `authenticationMethod` | `route` means authentication is handled per-route basis |
| `sessionTimeout` | Session expiration time in minutes |
| `destination` | Maps to a destination defined in MTA that points to the backend |
| `authenticationType` | `none` passes through requests; `xsuaa` requires OAuth token |

**📚 Documentation:**
- [SAP Application Router](https://help.sap.com/docs/btp/sap-business-technology-platform/application-router)
- [xs-app.json Configuration](https://help.sap.com/docs/btp/sap-business-technology-platform/routing-configuration-file)

**💡 Tips:**
- Use `authenticationType: none` for routes that handle their own authentication (like Spring Security)
- Disable CSRF protection for API routes if your backend handles it separately
- The `destination` name must match the destination defined in `mta.yaml`

---

### 2. [CF] Create xs-security.json with Multitenancy Support

Define OAuth2 security configuration with roles, scopes, and the critical `tenant-mode: shared` setting.

**xs-security.json:**
```json
{
  "xsappname": "learning-management-system",
  "tenant-mode": "shared",
  "description": "Learning Management System - OAuth2 Security Configuration with Multitenancy Support",
  "scopes": [
    {
      "name": "$XSAPPNAME.user",
      "description": "Basic user access"
    },
    {
      "name": "$XSAPPNAME.admin",
      "description": "Administrator access"
    },
    {
      "name": "$XSAPPNAME.Callback",
      "description": "Callback scope for SaaS Provisioning Service",
      "grant-as-authority-to-apps": [
        "$XSAPPNAME(application,sap-provisioning,tenant-onboarding)"
      ]
    }
  ],
  "role-templates": [
    {
      "name": "User",
      "description": "Standard user with access to the application",
      "scope-references": ["$XSAPPNAME.user"]
    },
    {
      "name": "Admin",
      "description": "Administrator with full access including application info",
      "scope-references": ["$XSAPPNAME.user", "$XSAPPNAME.admin"]
    }
  ],
  "role-collections": [
    {
      "name": "LMS_User",
      "description": "LMS Application Users",
      "role-template-references": ["$XSAPPNAME.User"]
    },
    {
      "name": "LMS_Admin",
      "description": "LMS Application Administrators",
      "role-template-references": ["$XSAPPNAME.Admin"]
    }
  ],
  "authorities": [
    "$XSAPPNAME.user",
    "$XSAPPNAME.admin",
    "$XSAPPNAME.Callback"
  ],
  "oauth2-configuration": {
    "redirect-uris": ["https://*.cfapps.us10-001.hana.ondemand.com/**"],
    "token-validity": 900
  }
}
```

**Critical Settings for Multitenancy:**
| Setting | Value | Purpose |
|---------|-------|---------|
| `tenant-mode` | `shared` | **Required** for SaaS multitenancy. Creates isolated token contexts per tenant |
| `$XSAPPNAME.Callback` | Special scope | Authorizes SaaS Provisioning Service to call your subscription callbacks |
| `grant-as-authority-to-apps` | `sap-provisioning,tenant-onboarding` | Grants callback permission to SaaS Registry |

**⚠️ Important:**
- The `tenant-mode: shared` setting is **mandatory** for multitenant SaaS applications
- The `Callback` scope is required for the SaaS Provisioning Service to invoke your subscription endpoints
- Update `redirect-uris` to match your SAP BTP region (e.g., `us10-001`, `eu10`, `ap21`)

**📚 Documentation:**
- [XSUAA Application Security Descriptor](https://help.sap.com/docs/btp/sap-business-technology-platform/application-security-descriptor-configuration-syntax)
- [Multitenancy in XSUAA](https://help.sap.com/docs/btp/sap-business-technology-platform/developing-multitenant-applications-in-the-cloud-foundry-environment)

---

### 3. [CF] Create saas-provisioning.json for Marketplace Registration

Configure the SaaS Provisioning Service to register your application in the marketplace.

**saas-provisioning.json:**
```json
{
  "xsappname": "learning-management-system",
  "appName": "learning-management-system",
  "displayName": "Learning Management System",
  "description": "LMS - A multi-tenant learning management system for course management",
  "category": "Education",
  "appUrls": {
    "getDependencies": "~{lms-api/url}/callback/v1.0/dependencies",
    "onSubscription": "~{lms-api/url}/callback/v1.0/tenants/{tenantId}",
    "onSubscriptionAsync": false,
    "onUnSubscriptionAsync": false
  }
}
```

**Configuration Properties:**
| Property | Description |
|----------|-------------|
| `xsappname` | Must match the value in `xs-security.json` |
| `appName` | Unique application name for the marketplace |
| `displayName` | Human-readable name shown in the marketplace UI |
| `category` | Marketplace category for discoverability |
| `getDependencies` | Callback to list required service dependencies |
| `onSubscription` | Callback invoked when a tenant subscribes (PUT) and unsubscribes (DELETE) |
| `onSubscriptionAsync` | Set to `true` for long-running subscription processes |

**📚 Documentation:**
- [SaaS Provisioning Service](https://help.sap.com/docs/btp/sap-business-technology-platform/using-saas-provisioning-service-to-register-your-multitenant-application)
- [Subscription Callback Parameters](https://help.sap.com/docs/btp/sap-business-technology-platform/subscription-callbacks)

---

### 4. [JAVA] Implement Tenant Provisioning Callbacks

Create the controller that handles subscription lifecycle events from the SaaS Provisioning Service.

**Subscription Lifecycle Flow:**

```
┌────────────────────┐     ┌─────────────────────┐     ┌────────────────────┐
│  Subscriber        │     │  SaaS Provisioning  │     │  Your Application  │
│  Subaccount        │     │  Service            │     │  (Callbacks)       │
└────────────────────┘     └─────────────────────┘     └────────────────────┘
         │                           │                           │
         │  1. Subscribe via        │                           │
         │     BTP Cockpit          │                           │
         │─────────────────────────>│                           │
         │                           │  2. GET /dependencies     │
         │                           │─────────────────────────>│
         │                           │<─────────────────────────│
         │                           │     []                    │
         │                           │                           │
         │                           │  3. PUT /tenants/{id}     │
         │                           │─────────────────────────>│
         │                           │     {subscribedSubdomain} │
         │                           │                           │
         │                           │     4. Create schema      │
         │                           │     5. Return tenant URL  │
         │                           │<─────────────────────────│
         │                           │     https://tenant.app... │
         │                           │                           │
         │  6. Subscription         │                           │
         │     Complete             │                           │
         │<─────────────────────────│                           │
```

**⚠️ Important:**
- The subscription callback MUST return a valid tenant-specific URL as **plain text** (not JSON)
- For SAP BTP Trial, you need to manually map routes for each tenant:
  ```bash
  cf map-route lms-approuter cfapps.us10-001.hana.ondemand.com --hostname <subdomain>
  ```
- The `tenantId` is the subscriber subaccount's GUID, not a human-readable name

**📚 Documentation:**
- [Implementing Subscription Callbacks](https://help.sap.com/docs/btp/sap-business-technology-platform/subscription-callbacks)
- [Multitenant Applications Development](https://help.sap.com/docs/btp/sap-business-technology-platform/developing-multitenant-applications-in-the-cloud-foundry-environment)

---

### 5. [CF] Configure MTA Deployment Descriptor

Update `mta.yaml` to include the approuter module and SaaS Registry service.

**mta.yaml (Key Sections):**
```yaml
_schema-version: "3.2"
ID: learning-management-system
version: 0.1.0

modules:
  # Approuter Module - Entry point for all requests
  - name: lms-approuter
    type: approuter.nodejs
    path: approuter
    build-parameters:
      builder: npm
    parameters:
      memory: 256M
      disk-quota: 256M
      instances: 1
    properties:
      # Pattern to extract tenant subdomain from URL
      TENANT_HOST_PATTERN: "^(.*)-lms-approuter.cfapps.us10-001.hana.ondemand.com"
    provides:
      - name: lms-approuter-url
        properties:
          url: ${default-url}
    requires:
      - name: lms-xsuaa
      - name: lms-api
        group: destinations
        properties:
          name: lms-backend
          url: ~{url}
          forwardAuthToken: true

  # Backend Java Application
  - name: learning-management-system
    type: java
    path: .
    parameters:
      memory: 1024M
      buildpack: sap_java_buildpack_jakarta
    properties:
      SPRING_PROFILES_ACTIVE: cloud
      APPROUTER_URL: ~{lms-approuter-url/url}
    provides:
      - name: lms-api
        properties:
          url: ${default-url}
    requires:
      - name: lms-xsuaa
      - name: lms-saas-registry
      - name: lms-approuter-url
      # ... other services

resources:
  # XSUAA Service - MUST use 'broker' plan for SaaS multitenancy
  - name: lms-xsuaa
    type: org.cloudfoundry.managed-service
    parameters:
      service: xsuaa
      service-plan: broker  # 'broker' plan required for multitenancy!
      service-name: lms-xsuaa
      path: xs-security.json

  # SaaS Provisioning Service - Marketplace registration
  - name: lms-saas-registry
    type: org.cloudfoundry.managed-service
    parameters:
      service: saas-registry
      service-plan: application
      service-name: lms-saas-registry
      config:
        xsappname: learning-management-system
        appName: learning-management-system
        displayName: Learning Management System
        description: LMS - A multi-tenant learning management system
        category: Education
        appUrls:
          getDependencies: ~{lms-api/url}/callback/v1.0/dependencies
          onSubscription: ~{lms-api/url}/callback/v1.0/tenants/{tenantId}
          onSubscriptionAsync: false
          onUnSubscriptionAsync: false
    requires:
      - name: lms-api
```

**Critical MTA Configuration Points:**
| Element | Purpose |
|---------|---------|
| `TENANT_HOST_PATTERN` | Regex to extract tenant subdomain from incoming requests |
| `service-plan: broker` | **Required** for XSUAA in multitenant apps (not `application`) |
| `forwardAuthToken: true` | Passes OAuth token from approuter to backend |
| `~{lms-api/url}` | MTA variable syntax - resolves to backend URL at deploy time |

**⚠️ Important:**
- **XSUAA must use the `broker` plan** for multitenant SaaS applications, not the `application` plan
- The `TENANT_HOST_PATTERN` must match your approuter URL pattern
- Callback URLs use the `~{lms-api/url}` variable to reference the backend URL dynamically

**📚 Documentation:**
- [MTA Deployment Descriptor](https://help.sap.com/docs/btp/sap-business-technology-platform/multitarget-application-mta-model)
- [MTA Module Types](https://help.sap.com/docs/btp/sap-business-technology-platform/modules)

---

### 6. [CF] Deploy and Create Subscriber Subaccount

Deploy the application and create a subscriber subaccount to test subscription.

**Build and Deploy:**
```bash
# Build the Java application
mvn clean package -P cloud -DskipTests

# Build the MTA archive
mbt build

# Deploy to Cloud Foundry
cf deploy mta_archives/learning-management-system_0.1.0.mtar
```

**Create Subscriber Subaccount (BTP Cockpit):**
1. Navigate to your Global Account in BTP Cockpit
2. Click **Account Explorer** → **Create** → **Subaccount**
3. Enter:
   - **Display Name:** e.g., "LMS Tenant A"
   - **Subdomain:** e.g., "tenant-a" (will be part of the URL)
   - **Region:** Same as provider subaccount
4. Click **Create**

**Subscribe to the Application:**
1. Open the newly created subscriber subaccount
2. Navigate to **Services** → **Instances and Subscriptions**
3. Click **Create**
4. Select **Service:** Your application (e.g., "Learning Management System")
5. Click **Create**

**Verify Subscription:**
```bash
# Check application logs for subscription callback
cf logs learning-management-system --recent | grep -i subscription

# Expected log output:
# Tenant subscription request received for tenantId: <guid>
# Tenant <guid> subscribed successfully. URL: https://tenant-a.cfapps...
```

**Map Tenant Route (Required for BTP Trial):**
```bash
# Map a route for the new tenant
cf map-route lms-approuter cfapps.us10-001.hana.ondemand.com --hostname tenant-a

# Verify the route
cf routes
```

**Access the Application:**
- Provider URL: `https://<org>-<space>-lms-approuter.cfapps.us10-001.hana.ondemand.com`
- Tenant URL: `https://tenant-a.cfapps.us10-001.hana.ondemand.com`

**✅ Verification Checklist:**
- [ ] Application deploys without errors
- [ ] SaaS Registry shows your app in the marketplace
- [ ] Subscriber subaccount created successfully
- [ ] Subscription completes without errors
- [ ] Tenant-specific URL is accessible
- [ ] `/api/v1/application-info` returns XSUAA credentials (with ADMIN token)

---

## Common Issues and Solutions

### Subscription Callback Fails

**Symptom:** Subscription fails with "Subscription callback returned an error"

**Solutions:**
1. Check that your callback endpoints are publicly accessible
2. Verify the `Callback` scope is correctly defined in `xs-security.json`
3. Check application logs: `cf logs <app-name> --recent`
4. Ensure callback returns **plain text URL**, not JSON

### 403 Forbidden on Subscription

**Symptom:** SaaS Registry gets 403 when calling your callbacks

**Solution:** Ensure `xs-security.json` includes the Callback scope:
```json
{
  "name": "$XSAPPNAME.Callback",
  "grant-as-authority-to-apps": [
    "$XSAPPNAME(application,sap-provisioning,tenant-onboarding)"
  ]
}
```

### Tenant URL Not Accessible

**Symptom:** After subscription, tenant URL returns 404

**Solutions (BTP Trial):**
1. Map the route manually:
   ```bash
   cf map-route lms-approuter cfapps.<region>.hana.ondemand.com --hostname <subdomain>
   ```
2. Verify with: `cf routes | grep <subdomain>`

---

## Achievements

### [CF]
| Concept | Description |
|---------|-------------|
| SAP Approuter | Node.js application for centralized request handling and authentication |
| XSUAA Broker Plan | OAuth2 service plan required for multitenant SaaS applications |
| SaaS Provisioning Service | Service for marketplace registration and tenant lifecycle management |
| xs-security.json | OAuth2 security descriptor with scopes, roles, and tenant-mode |
| Subscription Callbacks | Endpoints invoked by SaaS Registry during tenant onboarding/offboarding |
| Tenant-Specific URLs | Subdomain-based routing for tenant isolation |
| VCAP_SERVICES | Cloud Foundry environment variable containing bound service credentials |

### [JAVA]
| Concept | Description |
|---------|-------------|
| `@Profile("cloud")` | Spring annotation to activate beans only in cloud environment |
| `@PreAuthorize` | Method-level security for role-based access control |
| `java-cfenv-boot` | Library for reading VCAP_SERVICES using CfEnv API |
| REST Callbacks | Implementing PUT/DELETE endpoints for tenant lifecycle |
| `TenantContext` | Thread-local storage for per-request tenant isolation |
| Hibernate Multi-tenancy | Schema-based isolation using `MultiTenantConnectionProvider` |

### [BTP]
| Concept | Description |
|---------|-------------|
| Provider Subaccount | Subaccount hosting the SaaS application |
| Subscriber Subaccount | Tenant subaccount that subscribes to the application |
| Marketplace | BTP catalog where applications can be discovered and subscribed |
| Route Mapping | Cloud Foundry mechanism for exposing tenant-specific URLs |

---

## Useful Resources

- [SAP BTP Multitenancy Concepts](https://help.sap.com/docs/btp/sap-business-technology-platform/multitenancy-development-concepts)
- [Developing Multitenant Applications (Tutorial)](https://developers.sap.com/mission.cp-cf-multitenancy-introduction.html)
- [SAP Application Router Documentation](https://www.npmjs.com/package/@sap/approuter)
- [XSUAA Security Configuration](https://help.sap.com/docs/btp/sap-business-technology-platform/application-security-descriptor-configuration-syntax)
- [SaaS Provisioning Service API](https://help.sap.com/docs/btp/sap-business-technology-platform/using-saas-provisioning-service-to-register-your-multitenant-application)
- [java-cfenv-boot Documentation](https://github.com/pivotal-cf/java-cfenv)

---

## Next Steps (Stage 6)

In **Stage 6 – SAP BTP Multitenancy (Advanced)**, you will:
- Implement **schema-based tenant isolation** using Hibernate multi-tenancy
- Create `TenantContext`, `TenantFilter`, and `TenantConnectionProvider` classes
- Configure Liquibase for per-tenant schema migrations
- Extract tenant ID from JWT tokens for automatic tenant resolution