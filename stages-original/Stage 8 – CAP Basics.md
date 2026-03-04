# Stage 8 – CAP Basics

# Functional requirement [FR]

* Create a new **CAP Java** project with Spring Boot in a separate `main-cap` branch
* Implement the same **domain model** as the main branch using CDS (Students, Courses, Lessons, Enrollments)
* Expose **OData V4** services with full CRUD operations
* Configure **OAuth 2.0** security using SAP XSUAA
* Expose **Spring Boot Actuator** endpoints for monitoring
* Deploy to SAP BTP Cloud Foundry with required services

## Steps
1. **[CAP]** Create new branch `main-cap` and initialize CAP Java project using `cds init --add java,hana,mta,xsuaa`
2. **[CAP]** Define CDS data model (`db/schema.cds`) with Students, Courses, Lessons, Enrollments entities
3. **[CAP]** Use `cuid` and `managed` aspects for UUID and audit fields
4. **[CAP]** Define OData service (`srv/service.cds`) with projections and custom actions
5. **[JAVA]** Implement custom event handlers for `enrollStudent` and `purchaseCourse` actions
6. **[CF]** Configure `xs-security.json` with scopes and roles (`tenant-mode: dedicated`)
7. **[SPRING]** Configure `application.yaml` with XSUAA security and Actuator endpoints
8. **[CF]** Configure `mta.yaml` with required services:
   - HANA Cloud (hdi-shared)
   - Application Logging
   - Application Autoscaler
   - Destination Service
   - Feature Flags Service
   - User-Provided Service (SMTP)
9. **[CF]** Build and deploy: `mvn clean install && mbt build && cf deploy`
10. **[TEST]** Create Postman collection for OData endpoints (local + cloud environments)


# Achievements

#### [CAP]

1. CDS data modeling (`schema.cds`)
2. CDS service definitions (`service.cds`)
3. Generic handlers (automatic CRUD)
4. Custom event handlers (`@On`, `@Before`, `@After`)
5. `cuid` aspect (UUID generation)
6. `managed` aspect (audit fields)
7. Associations and Compositions
8. `@requires` annotation for authorization

#### [JAVA]

1. EventHandler interface
2. `@ServiceName` annotation
3. CAP Java SDK

#### [SAP BTP]

1. HDI Container (hdi-shared)
2. MTA deployment for CAP
3. XSUAA with CAP

#### [ODATA]

1. OData V4 conventions
2. $expand, $filter, $orderby
3. Actions and Functions


## Advices

- [CAP Java Getting Started](https://cap.cloud.sap/docs/java/getting-started)
- [CDS Language Reference](https://cap.cloud.sap/docs/cds/)
- [CAP Best Practices](https://cap.cloud.sap/docs/guides/best-practices)