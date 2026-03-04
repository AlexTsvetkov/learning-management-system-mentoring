# Stage 9 – CAP Multitenancy

# Functional requirement [FR]

* Enable **multitenancy** in the CAP Java application from Stage 8
* Configure **SaaS Registry** for tenant subscription management
* Use CAP's built-in **MTX services** for automatic tenant provisioning
* Implement tenant-aware **data isolation** using HDI containers per tenant
* Add **Approuter** for tenant-specific URL routing
* Test subscription and unsubscription flows

## Steps
1. **[CAP]** Enable multitenancy in `package.json`: `cds.requires.multitenancy = true`
2. **[CAP]** Configure `application.yaml` with `cds.multitenancy.enabled: true`
3. **[CF]** Update `xs-security.json`:
   - Change `tenant-mode` from `dedicated` to `shared`
   - Add `mtcallback` scope for SaaS Provisioning Service
   - Add `mtdeployment` scope for HDI deployment
4. **[CF]** Change XSUAA service plan from `application` to `broker`
5. **[CF]** Add Service Manager resource (`service-plan: container`) for dynamic HDI provisioning
6. **[CF]** Add SaaS Registry resource with CAP MTX callback URLs:
   - `/-/cds/saas-provisioning/dependencies`
   - `/-/cds/saas-provisioning/tenant/{tenantId}`
7. **[NODE]** Create Approuter module (`approuter/`) with `xs-app.json` and `TENANT_HOST_PATTERN`
8. **[JAVA]** (Optional) Create custom `TenantSubscriptionHandler` for subscription events
9. **[CF]** Update `mta.yaml` with approuter module and SaaS Registry
10. **[CF]** Deploy and map tenant routes: `cf map-route <app> <domain> --hostname <subdomain>`
11. **[TEST]** Test subscription via BTP Cockpit
12. **[TEST]** Update Postman collection with provider and subscriber token flows


# Achievements

#### [CAP]

1. CAP MTX (Multitenancy Extension)
2. Built-in subscription callbacks (`/-/cds/saas-provisioning/`)
3. `TenantProviderService` for custom subscription handling
4. `RequestContext` for tenant information
5. Automatic tenant isolation

#### [SAP BTP]

1. XSUAA broker plan
2. Service Manager (container plan)
3. SaaS Registry (application plan)
4. Async subscription callbacks
5. HDI container per tenant

#### [ARCHITECTURE]

1. Schema-per-tenant isolation
2. Provider vs Subscriber subaccounts
3. Tenant-specific URL routing


## Advices

- [CAP Multitenancy Guide](https://cap.cloud.sap/docs/guides/multitenancy/)
- [CAP MTX Services](https://cap.cloud.sap/docs/guides/multitenancy/mtxs)
- [CAP Java Multitenancy](https://cap.cloud.sap/docs/java/multitenancy)