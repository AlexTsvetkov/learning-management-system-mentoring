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
