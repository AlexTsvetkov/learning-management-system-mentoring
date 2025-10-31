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

## Run the app
mvn spring-boot:run

## App runs at: http://localhost:8080

## Swagger UI
http://localhost:8080/swagger-ui.html

## Email Notifications

Uses Mailtrap (or any SMTP) to send notifications before course start.

## Daily Scheduled Job
Runs at midnight to:
Find all courses starting the next day.
Send email notifications to enrolled students using a custom thread pool.

## Maintenance
Liquibase keeps the schema consistent.
CHANGELOG.md documents project evolution.
Unit tests cover core business logic and controllers.