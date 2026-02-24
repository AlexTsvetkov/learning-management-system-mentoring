# Changelog

## [0.0.1] - Initial
- Project skeleton with Spring Boot 3.x and Java 21
- Entities: Student, Course, CourseSettings, Lesson
- CRUD endpoints for Student and Course
- Liquibase changelog for H2 in-memory DB
- Scheduled job to notify students about courses starting tomorrow using a custom thread pool
- Mail configuration (Mailtrap) + JavaMailSender usage
- MapStruct, Lombok, Swagger (OpenAPI), Actuator
- Basic unit test and error handler

## [0.0.2] - Updated
- StudentDto now includes full CourseDto list.
- StudentMapper uses CourseMapper to map courses.
- StudentService.findById annotated with @Transactional(readOnly = true) to allow lazy-loading before mapping.
- Added unit tests for services and controllers (plain method unit tests).

## [0.0.3] - 2025-11-20
- Improved entity model and mapping for the recent refactor.
- Added validation annotations to entity fields.
- Updated DTOs and mappers to reflect entity changes and avoid lazy-loading issues.
- Adjusted unit tests to cover the refactored behavior.
- Minor database migration updates (Liquibase) if schema changed.

## [0.0.4] - 2025-12-02
### Security
- Secured CRUD endpoints and actuator endpoints.
- Improved SecurityConfig implementation.

## [0.0.5] - 2026-02-09
### Features
- Added API versioning to endpoints (v1 prefix).
- Implemented new endpoint `GET /api/v1/students/{id}/courses` to retrieve student's courses.

### Testing
- Improved tests: removed redundant code and simplified assertions.
- Aligned tests naming and structure.
- Introduced `TestDataGenerator` utility for cleaner test code.
- Added basic credentials to Postman collection.

## [0.0.6] - 2026-02-13
### Cloud Deployment
- Switched to Java 17 for BTP trial deployment (java.buildpack compatibility).
- Prepared application for BTP deployment with cloud profile.
- Created cloud and local Postman environments.
- Updated health-check endpoint path in manifest.yaml.

### SMTP Configuration
- Implemented User-Provided Service for SMTP credentials.
- Added SMTP credentials providers (VCAP, Destination).
- Added `SmtpCredentialsResolver` for flexible credential resolution.

### Other
- Updated .gitignore configuration.

## [0.0.7] - 2026-02-18
### Security
- Secured API endpoints in cloud using XSUAA service with OAuth2 authentication.
- All endpoints now require authenticated users.

### Features
- Added Feature Flag service integration.
- Changed email notification cron schedule to once a day at 07:00.

### Fixes
- Various bug fixes and improvements.

## [0.0.8] - 2026-02-19
### CI/CD
- Updated GitLab CI pipeline to use dedicated runner.
- Improved pipeline script configuration.

## [0.0.9] - 2026-02-23
### Deployment
- Added MTA (Multi-Target Application) deployment support.
- Created `mta.yaml` and `mta-extension.mtaext` configuration files.
- Added DEPLOYMENT.md with MTA deployment instructions.

## [0.0.10] - 2026-02-24
### Features
- Implemented lesson type inheritance (VideoLesson, ClassroomLesson).
- Added i18n (internationalization) support for email templates (en, de, ru).
- Added JPA auditing for entity timestamps (createdAt, updatedAt).
- Implemented pagination support for API endpoints.

### Configuration
- Introduced `dev-postgres` profile for local PostgreSQL development.
- Updated Swagger/OpenAPI configuration.
- Cleaned up pom.xml dependencies.
- Added `java-cfenv-boot` for cloud environment configuration.

### Testing
- Added comprehensive tests for SMTP components (SmtpCredentialsResolver, SmtpCredentials, VcapSmtpCredentialsProvider).
- Fixed integration tests configuration.
- Updated README.md with testing instructions.

### Documentation
- Updated Postman collection for local environment.
- Updated README.md with new features and setup instructions.

