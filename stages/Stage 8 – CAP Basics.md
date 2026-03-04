# Stage 8 – CAP Basics

> **Estimated Duration:** 2-3 weeks  
> **Prerequisites:** Stage 1-4, Basic understanding of SAP CAP  
> **Branch:** `main-cap`

## Overview

In this stage, you will create a **SAP Cloud Application Programming Model (CAP)** version of the Learning Management System using **CAP Java with Spring Boot**. CAP is SAP's recommended framework for building enterprise-grade services and applications.

This is a **parallel implementation** to the Spring Boot app in the main branch, showcasing how CAP simplifies development with:
- Declarative data modeling with CDS (Core Data Services)
- Built-in OData V4 support
- Automatic CRUD operations (generic handlers)
- Simplified service definitions

## Functional Requirements [FR]

* Create a new CAP Java project with Spring Boot in a separate `main-cap` branch
* Implement the same domain model as the main branch:
  - **Students** entity with CRUD operations
  - **Courses** entity with CRUD operations  
  - **Enrollments** (student-course relationship) with CRUD operations
* Expose OData V4 services for all entities
* Configure OAuth 2.0 security using SAP XSUAA
* Expose Spring Boot Actuator endpoints for monitoring
* Deploy to SAP BTP Cloud Foundry with required services

## Required Services

| Service | Purpose |
|---------|---------|
| HANA Cloud (hdi-shared) | Database for CAP entities |
| Application Logging | Centralized logging (Kibana) |
| Application Autoscaler | Auto-scaling based on metrics |
| Destination Service | External service connectivity |
| Feature Flags Service | Feature toggles |
| User-Provided Service (SMTP) | Email configuration |

---

## Steps

### 1. Create CAP Java Project

Create a new branch and initialize a CAP Java project:

```bash
# Create new branch
git checkout -b main-cap

# Create CAP Java project with Spring Boot
cds init lms-cap --add java,hana,mta,xsuaa

# Navigate to project
cd lms-cap
```

Alternative using Maven archetype:
```bash
mvn archetype:generate -DarchetypeArtifactId=cds-services-archetype \
    -DarchetypeGroupId=com.sap.cds \
    -DarchetypeVersion=RELEASE \
    -DgroupId=com.lms.mentoring \
    -DartifactId=lms-cap
```

### 2. Define Data Model (schema.cds)

Create the CDS data model mirroring the existing JPA entities.

**db/schema.cds:**
```cds
namespace lms;

using { cuid, managed } from '@sap/cds/common';

/**
 * Student entity
 */
entity Students : cuid, managed {
    firstName    : String(100) @mandatory;
    lastName     : String(100) @mandatory;
    email        : String(255) @mandatory;
    dateOfBirth  : Date @mandatory;
    coins        : Decimal(19, 2) default 0;
    locale       : String(10) default 'en';
    courses      : Association to many Enrollments on courses.student = $self;
}

/**
 * Course entity
 */
entity Courses : cuid, managed {
    title        : String(255) @mandatory;
    description  : String(2000);
    price        : Decimal(19, 2) default 0 @mandatory;
    coinsPaid    : Decimal(19, 2) default 0;
    startDate    : Date;
    students     : Association to many Enrollments on students.course = $self;
    lessons      : Composition of many Lessons on lessons.course = $self;
}

/**
 * Course Settings (1:1 with Course)
 */
entity CourseSettings : cuid {
    course              : Association to one Courses;
    notifyBeforeStart   : Boolean default true;
    notifyDaysBefore    : Integer default 1;
    maxEnrollments      : Integer default 100;
}

/**
 * Lesson entity (composition of Course)
 */
entity Lessons : cuid, managed {
    title       : String(255) @mandatory;
    content     : LargeString;
    duration    : Integer; // in minutes
    course      : Association to one Courses @mandatory;
    lessonType  : String(20) enum { VIDEO; CLASSROOM; } default 'VIDEO';
    // Type-specific fields
    videoUrl    : String(500);  // for VIDEO type
    roomNumber  : String(50);   // for CLASSROOM type
    instructor  : String(100);  // for CLASSROOM type
}

/**
 * Student-Course enrollment (many-to-many)
 */
entity Enrollments : cuid, managed {
    student     : Association to one Students @mandatory;
    course      : Association to one Courses @mandatory;
    enrolledAt  : Timestamp @cds.on.insert: $now;
    completed   : Boolean default false;
}
```

### 3. Define Service (service.cds)

Create an OData service exposing the entities.

**srv/service.cds:**
```cds
using { lms } from '../db/schema';

/**
 * Learning Management Service - Main OData V4 Service
 */
@path: '/api/v1'
service LearningManagementService @(requires: 'authenticated-user') {

    @odata.draft.enabled  // Enable draft support for UI5 apps
    entity Students as projection on lms.Students {
        *,
        courses : Association to many Enrollments on courses.student = $self
    };

    @odata.draft.enabled
    entity Courses as projection on lms.Courses {
        *,
        students : Association to many Enrollments on students.course = $self,
        lessons  : Composition of many Lessons on lessons.course = $self
    };

    entity Lessons as projection on lms.Lessons;
    entity Enrollments as projection on lms.Enrollments;
    entity CourseSettings as projection on lms.CourseSettings;

    // Custom actions
    action enrollStudent(studentId: UUID, courseId: UUID) returns Enrollments;
    action purchaseCourse(studentId: UUID, courseId: UUID) returns { success: Boolean; message: String };
    
    // Custom functions
    function getStudentCourses(studentId: UUID) returns array of Courses;
}

/**
 * Admin Service - Restricted to admin role
 */
@path: '/api/admin'
@requires: 'admin'
service AdminService {
    entity Students as projection on lms.Students;
    entity Courses as projection on lms.Courses;
    entity Enrollments as projection on lms.Enrollments;
}
```

### 4. Implement Custom Handlers (Java)

Create Java handlers for custom business logic.

**srv/src/main/java/com/lms/mentoring/handlers/LearningManagementServiceHandler.java:**
```java
package com.lms.mentoring.handlers;

import com.sap.cds.services.cds.CdsService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

import cds.gen.learningmanagementservice.*;
import cds.gen.lms.Courses;
import cds.gen.lms.Students;
import cds.gen.lms.Enrollments;

import java.math.BigDecimal;

@Component
@ServiceName(LearningManagementService_.CDS_NAME)
public class LearningManagementServiceHandler implements EventHandler {

    @On(event = EnrollStudentContext.CDS_NAME)
    public void onEnrollStudent(EnrollStudentContext context) {
        // Custom enrollment logic
        String studentId = context.getStudentId();
        String courseId = context.getCourseId();
        
        // Implementation here
        Enrollments enrollment = Enrollments.create();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        
        context.setResult(enrollment);
    }

    @On(event = PurchaseCourseContext.CDS_NAME)
    public void onPurchaseCourse(PurchaseCourseContext context) {
        // Coin-based purchase logic
        String studentId = context.getStudentId();
        String courseId = context.getCourseId();
        
        // Implementation similar to CoursePurchaseService
        PurchaseCourseContext.Result result = PurchaseCourseContext.Result.create();
        result.setSuccess(true);
        result.setMessage("Course purchased successfully");
        
        context.setResult(result);
    }
}
```

### 5. Configure Security (xs-security.json)

**xs-security.json:**
```json
{
    "xsappname": "lms-cap",
    "tenant-mode": "dedicated",
    "scopes": [
        { "name": "$XSAPPNAME.user", "description": "User access" },
        { "name": "$XSAPPNAME.admin", "description": "Admin access" }
    ],
    "role-templates": [
        { "name": "User", "scope-references": ["$XSAPPNAME.user"] },
        { "name": "Admin", "scope-references": ["$XSAPPNAME.user", "$XSAPPNAME.admin"] }
    ],
    "role-collections": [
        { "name": "LMS_CAP_User", "role-template-references": ["$XSAPPNAME.User"] },
        { "name": "LMS_CAP_Admin", "role-template-references": ["$XSAPPNAME.Admin"] }
    ],
    "oauth2-configuration": {
        "redirect-uris": ["https://*.cfapps.us10-001.hana.ondemand.com/**"]
    }
}
```

### 6. Configure Application Properties

**srv/src/main/resources/application.yaml:**
```yaml
spring:
  application:
    name: lms-cap

cds:
  security:
    xsuaa:
      enabled: true
  odata-v4:
    endpoint:
      path: /odata/v4
  
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  health:
    db:
      enabled: true

logging:
  level:
    com.sap.cds: INFO
    com.lms.mentoring: DEBUG

---
spring:
  config:
    activate:
      on-profile: cloud

cds:
  datasource:
    hana:
      enabled: true
```

### 7. Configure MTA Deployment (mta.yaml)

**mta.yaml:**
```yaml
_schema-version: "3.2"
ID: lms-cap
version: 1.0.0
description: Learning Management System - CAP Java

modules:
  - name: lms-cap-srv
    type: java
    path: srv
    parameters:
      memory: 1024M
      buildpack: sap_java_buildpack_jakarta
    properties:
      SPRING_PROFILES_ACTIVE: cloud
    provides:
      - name: srv-api
        properties:
          srv-url: ${default-url}
    requires:
      - name: lms-cap-db
      - name: lms-cap-xsuaa
      - name: lms-cap-logging
      - name: lms-cap-destination
      - name: lms-cap-feature-flags
      - name: lms-cap-smtp-credentials
      - name: lms-cap-autoscaler

  - name: lms-cap-db-deployer
    type: hdb
    path: db
    parameters:
      buildpack: nodejs_buildpack
    requires:
      - name: lms-cap-db

resources:
  - name: lms-cap-db
    type: com.sap.xs.hdi-container
    parameters:
      service: hana
      service-plan: hdi-shared

  - name: lms-cap-xsuaa
    type: org.cloudfoundry.managed-service
    parameters:
      service: xsuaa
      service-plan: application
      path: xs-security.json

  - name: lms-cap-logging
    type: org.cloudfoundry.managed-service
    parameters:
      service: application-logs
      service-plan: lite

  - name: lms-cap-destination
    type: org.cloudfoundry.managed-service
    parameters:
      service: destination
      service-plan: lite

  - name: lms-cap-feature-flags
    type: org.cloudfoundry.managed-service
    parameters:
      service: feature-flags
      service-plan: lite

  - name: lms-cap-autoscaler
    type: org.cloudfoundry.managed-service
    parameters:
      service: autoscaler
      service-plan: standard

  - name: lms-cap-smtp-credentials
    type: org.cloudfoundry.user-provided-service
    parameters:
      config:
        host: smtp.mailtrap.io
        port: "587"
        username: your-username
        password: your-password
```

### 8. Build and Deploy

```bash
# Build the project
mvn clean install

# Build MTA archive
mbt build

# Login to Cloud Foundry
cf login -a https://api.cf.us10-001.hana.ondemand.com

# Deploy
cf deploy mta_archives/lms-cap_1.0.0.mtar
```

### 9. Create Postman Collection

Create a Postman collection for testing the OData services locally and in the cloud.

**postman/CAP Local Environment:**
- `baseUrl`: `http://localhost:8080`
- `odataPath`: `/odata/v4/LearningManagementService`

**postman/CAP Cloud Environment:**
- `baseUrl`: `https://your-app.cfapps.us10-001.hana.ondemand.com`
- `tokenUrl`: from XSUAA credentials
- `clientId`: from XSUAA credentials
- `clientSecret`: from XSUAA credentials

**Sample Requests:**
- GET `{{baseUrl}}{{odataPath}}/Students`
- GET `{{baseUrl}}{{odataPath}}/Courses?$expand=lessons`
- POST `{{baseUrl}}{{odataPath}}/Students` (create)
- PATCH `{{baseUrl}}{{odataPath}}/Students('uuid')` (update)
- DELETE `{{baseUrl}}{{odataPath}}/Students('uuid')`

---

## Achievements

### [CAP]
| Concept | Description |
|---------|-------------|
| CDS Data Model | Declarative entity definitions with associations and compositions |
| CDS Service Definition | OData service exposure with projections |
| Generic Handlers | Automatic CRUD operations provided by CAP |
| Custom Handlers | Java event handlers for business logic |
| `@requires` Annotation | Role-based access control on services/entities |
| Managed Aspect | Automatic audit fields (createdAt, modifiedAt, etc.) |

### [SAP BTP]
| Concept | Description |
|---------|-------------|
| HDI Container | HANA Deployment Infrastructure for CAP |
| MTA Deployment | Multi-Target Application packaging |
| XSUAA Integration | OAuth 2.0 security with CAP |

---

## Useful Resources

- [CAP Java Documentation](https://cap.cloud.sap/docs/java/)
- [CDS Language Reference](https://cap.cloud.sap/docs/cds/)
- [CAP Java Event Handlers](https://cap.cloud.sap/docs/java/event-handlers/)
- [CAP Security](https://cap.cloud.sap/docs/java/security)
- [CAP with Spring Boot](https://cap.cloud.sap/docs/java/spring-boot-integration)

---

## Next Steps (Stage 9)

In **Stage 9 – CAP Multitenancy**, you will:
- Enable multitenant mode in CAP
- Configure SaaS Registry for subscription management
- Implement tenant-aware data isolation
- Update xs-security.json for shared tenant mode