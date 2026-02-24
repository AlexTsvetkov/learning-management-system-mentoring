# Learning Management System

A **Spring Boot 3 / Java 21** application for managing students, courses, and lessons — featuring clean architecture, modern tooling, and production-ready configuration.

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
| **Testing**     | JUnit 5, Mockito, Jacoco                     |

---

## Features

- Full **CRUD** for Students and Courses
- **Many-to-Many** relationship: Students ↔ Courses
- **One-to-One** Course ↔ CourseSettings
- **One-to-Many** Course ↔ Lessons with **JPA Inheritance**:
  - `Lesson` (base class)
  - `ClassroomLesson` (location, capacity)
  - `VideoLesson` (url, platform)
- Students can **purchase courses using coins** (balance validation included)
- **Pagination** support for all GET endpoints
- **Email localization** with locale field on Student (en, de, ru)
- **Audit fields** on entities (created, createdBy, lastChanged, lastChangedBy)
- **Daily scheduled job** to:
    - Identify courses starting tomorrow
    - Send notification emails to enrolled students using a custom thread pool
- **REST API documentation** via Swagger
- **Liquibase**-based schema versioning
- **Spring Boot Actuator** for health and info endpoints

---

## Run the App

### Default (H2 in-memory database)
```bash
mvn spring-boot:run
```

### With Dev Profile (H2 Console enabled)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### With PostgreSQL (dev-postgres profile)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev-postgres
```

App runs at: **http://localhost:8080**

---

## 🐘 PostgreSQL Development Setup

For local development with PostgreSQL, use the `dev-postgres` profile.

### 1. Start PostgreSQL with Docker Compose

```bash
docker-compose up -d
```

This starts a PostgreSQL 16 container with:
- **Database:** `lms_db`
- **Username:** `lms_user`
- **Password:** `lms_pass` (or empty with trust auth)
- **Port:** `5432`

### 2. Run Application with PostgreSQL Profile

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev-postgres
```

### 3. Stop PostgreSQL

```bash
docker-compose down
```

To also remove the data volume:
```bash
docker-compose down -v
```

### Profile Configuration

The `dev-postgres` profile in `application.yml`:

```yaml
spring:
  config:
    activate:
      on-profile: dev-postgres
  datasource:
    url: jdbc:postgresql://localhost:5432/lms_db
    username: lms_user
    password:
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
```

---

## ⚙️ Configuration Profiles

| Profile | Database | Use Case |
|---------|----------|----------|
| `default` | H2 (in-memory) | Quick local testing |
| `dev` | H2 (in-memory) | Development with H2 Console |
| `dev-postgres` | PostgreSQL | Development with persistent database |
| `cloud` | SAP HANA | SAP BTP Cloud Foundry deployment |
| `test` | H2 (in-memory) | Unit and integration tests |

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
1. Start the application: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
2. Open [http://localhost:8080/h2-console](http://localhost:8080/h2-console) in your browser
3. Fill in the connection settings:
   - **JDBC URL:** `jdbc:h2:mem:testdb`
   - **User Name:** `sa`
   - **Password:** *(leave empty)*
4. Click "Connect"
5. You can now browse tables and execute SQL queries

> **Note:** H2 Console is only available with the `dev` profile. It is disabled in cloud/production environments.

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

---

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify -DskipUTs
```

### Run All Tests with Coverage
```bash
mvn verify
```

Coverage reports are generated in:
- `target/site/jacoco-ut/` - Unit test coverage
- `target/site/jacoco-it/` - Integration test coverage
- `target/site/jacoco-merged/` - Merged coverage

---

## Email Notifications

Uses Mailtrap (or any SMTP) to send notifications before course start.

### Email Localization
Emails are sent in the student's preferred language based on the `locale` field:
- `en` - English (default)
- `de` - German
- `ru` - Russian

Templates are located in `src/main/resources/templates/email/`.

---

## Daily Scheduled Job
Runs at midnight to:
- Find all courses starting the next day
- Send email notifications to enrolled students using a custom thread pool

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

### Local Development

java-cfenv-boot has **no effect** when:
- Running locally (no `VCAP_SERVICES` environment variable)
- The environment variable is empty

This allows local H2/PostgreSQL configurations to work seamlessly.

---

## 🚀 Cloud Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed SAP BTP Cloud Foundry deployment instructions.

---

## Maintenance
- **Liquibase** keeps the schema consistent
- **CHANGELOG.md** documents project evolution
- Unit tests cover core business logic and controllers
- Integration tests verify repository layer with database