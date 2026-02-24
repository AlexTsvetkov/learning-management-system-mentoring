# Spring School Project

A **Spring Boot 3 / Java 21** application for managing students and courses — featuring clean architecture, modern
tooling, and production-ready configuration.

---

## Tech Stack

| Layer           | Technology                                   |
|-----------------|----------------------------------------------|
| **Language**    | Java 21                                      |
| **Framework**   | Spring Boot 3 (MVC, Data JPA, Security, AOP) |
| **ORM**         | Hibernate / JPA                              |
| **Database**    | H2 (in-memory), PostgreSQL, SAP HANA         |
| **Cloud**       | SAP BTP Cloud Foundry, java-cfenv-boot       |
| **Migration**   | Liquibase                                    |
| **API Docs**    | Swagger / Springdoc OpenAPI                  |
| **Templating**  | Mustache                                     |
| **Mapping**     | MapStruct                                    |
| **Boilerplate** | Lombok                                       |
| **Scheduler**   | Spring Scheduler + Custom ThreadPoolExecutor |
| **Testing**     | JUnit 5, Mockito                             |

---

## Features

- Full **CRUD** for Students and Courses
- **Many-to-Many** relationship: Students ↔ Courses
- **One-to-One** Course ↔ CourseSettings
- **One-to-Many** Course ↔ Lessons
- Students can **purchase courses using coins** (balance validation included)
- **Daily scheduled job** to:
    - Identify courses starting tomorrow
    - Send notification emails to enrolled students using a custom thread pool
- **REST API documentation** via Swagger
- **Liquibase**-based schema versioning
- **Spring Boot Actuator** for health and info endpoints

---


---

## ⚙️ Configuration

Default DB is **H2 (in-memory)** for easy local development.

`src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        show_sql: true
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml

springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true

logging:
  level:
    root: INFO

```

## Run the App

### Default (no profile)
```bash
mvn spring-boot:run
```

### With Dev Profile (H2 Console enabled)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

App runs at: **http://localhost:8080**

---

## 🔐 Local Development Authentication

For local development, the application uses Basic Authentication with in-memory users:

| Username | Password | Roles | Access |
|----------|----------|-------|--------|
| `user` | `password` | USER | API endpoints |
| `manager` | `secret` | USER, MANAGER | API + Actuator endpoints |

---

## 📖 Swagger UI (API Documentation)

Interactive API documentation is available via Swagger UI.

**URL:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

**Alternative URL:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

**OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### How to Use:
1. Start the application: `mvn spring-boot:run`
2. Open [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) in your browser
3. Click "Authorize" button (lock icon 🔒)
4. Enter credentials:
   - Username: `user`
   - Password: `password`
5. Click "Authorize" to authenticate
6. Now you can test all API endpoints directly from the browser

> **Note:** Swagger UI is a public endpoint and doesn't require authentication to view. However, executing API requests requires authentication.

---

## 🗄️ H2 Database Console

The H2 in-memory database console allows you to view and query the database directly in your browser.

**URL:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

### Connection Settings:
| Setting | Value |
|---------|-------|
| **JDBC URL** | `jdbc:h2:mem:testdb` |
| **User Name** | `sa` |
| **Password** | *(leave empty)* |

### How to Use:
1. Start the application: `mvn spring-boot:run`
2. Open [http://localhost:8080/h2-console](http://localhost:8080/h2-console) in your browser
3. Fill in the connection settings:
   - **JDBC URL:** `jdbc:h2:mem:testdb`
   - **User Name:** `sa`
   - **Password:** *(leave empty)*
4. Click "Connect"
5. You can now browse tables and execute SQL queries

> **Note:** H2 Console is only available in local/development mode. It is disabled in cloud/production environments.

---

## 📬 Postman Collections

Postman collections are available in the `postman/` directory:

| File | Description |
|------|-------------|
| `LMS Mentoring API - Local.postman_collection.json` | Local development (Basic Auth) |
| `LMS Mentoring API.postman_collection.json` | Cloud deployment (OAuth2/XSUAA) |
| `local.postman_environment.json` | Environment variables for local |
| `cloud.postman_environment.json` | Environment variables for cloud |

### Import into Postman:
1. Open Postman
2. Click "Import" → Select files from `postman/` directory
3. Select the appropriate environment (`local` or `cloud`)
4. Start testing APIs

## Email Notifications

Uses Mailtrap (or any SMTP) to send notifications before course start.

## Daily Scheduled Job
Runs at midnight to:
Find all courses starting the next day.
Send email notifications to enrolled students using a custom thread pool.

---

## ☁️ Cloud Foundry Integration (java-cfenv-boot)

The application uses **java-cfenv-boot** for automatic Cloud Foundry service binding configuration.

### What java-cfenv-boot Does

When deployed to SAP BTP Cloud Foundry, bound services inject credentials into the `VCAP_SERVICES` environment variable. java-cfenv-boot automatically:

1. **Parses `VCAP_SERVICES`** - Reads service binding JSON from the environment
2. **Auto-configures Spring Boot properties** - Maps credentials to `spring.datasource.*`, etc.
3. **Activates the `cloud` profile** - Automatically when running on Cloud Foundry

### Auto-Configured Services

| Service Type | Spring Boot Properties |
|--------------|------------------------|
| SAP HANA | `spring.datasource.url`, `username`, `password`, `driver-class-name` |
| PostgreSQL | `spring.datasource.url`, `username`, `password` |
| Other databases | Same pattern |

### Example: Zero-Config Database

Without java-cfenv-boot:
```yaml
spring:
  datasource:
    url: ${vcap.services.my-hana.credentials.url}
    username: ${vcap.services.my-hana.credentials.username}
    password: ${vcap.services.my-hana.credentials.password}
```

With java-cfenv-boot: **No manual configuration needed!**

The library automatically detects the SAP HANA service binding and configures Spring Boot.

### Local Development

java-cfenv-boot has **no effect** when:
- Running locally (no `VCAP_SERVICES` environment variable)
- The environment variable is empty

This allows local H2/PostgreSQL configurations to work seamlessly.

---

## Maintenance
Liquibase keeps the schema consistent.
CHANGELOG.md documents project evolution.
Unit tests cover core business logic and controllers.
