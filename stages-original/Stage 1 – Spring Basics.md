# Stage 1 – Spring Basics

# Functional requirement [FR]

* The system should provide full **CRUD** functionality for managing [domain models](../Onboarding%20Project%20-%20Learning%20Management%20System.md#domain-models). 
* It must run a **daily** automated job to identify courses starting the next day and send email notifications to all enrolled students using a dedicated thread pool. 
* The system needs to allow students to purchase courses using an internal currency, coins, verifying that they have a sufficient balance before the transaction.

## Steps
1. **[SPRING]** Create Spring Boot project backbone with `pom.xml`
2. **[DB]** Setup database schema using Liquibase and configure `H2 in-memory` database
3. **[DB]** CRUD for Students and Courses (Use Lombok and mapping tools (e.g. Mapstruct))
4. **[SPRING]** Add error handler
5. **[WEB]** Enable `Spring Boot Actuator` (health, info, logger)  + Logs
6. **[WEB]** Add `Swagger` for REST API
7. **[WEB]** Add validation for DTOs  
8. **[SPRING]** Disable `Open Session in View` (OSIV) feature and try to retest API
9. **[FR]** Implement job triggered daily to collect a list of Courses that start tomorrow
10. **[JOB]** Create a custom thread pool and send each email using threads from it
11. **[FR]** Implement logic for sending a notification message via `email` to enrolled Students (e.g. using [Mailtrap](https://mailtrap.io/)) 
12. **[FR]** Implement logic for coin-based payment (Student buys Courses using coins)
13. **[TEST]** Cover logic with unit and integration tests
14. **[VERSION]** Create `CHANGELOG.md` file and fill with actual changes description (Remember to update it after each stage!)


# Achievements

#### [SPRING]

1. Spring Boot Actuator  
2. OSIV
3. Swagger 
4. Validation
5. @RestController
6. @RestControllerAdvice
7. @ConfigurationProperties
8. @Transactional
9. @Async
10. @Lock
11. @Scheduled

#### [WEB]

1. REST API conventions  

#### [DB]

1. JPA/Hibernate
2. Transaction isolation level  
3. Eager/Lazy fetch types  
4. N+1 problem
5. Liquibase  

#### [TOOLING]
1. Mapstruct 
2. Lombok

#### [TEST]
1. JUnit  
2. Mockito  

#### [VERSION]
1. CHANGELOG.md

## Advices

- [Value Objects](https://dev.to/kirekov/spring-boot-power-of-value-objects-1oah)
- [Logging Strategies](https://www.papertrail.com/solution/tips/logging-in-java-best-practices-and-tips/) 
