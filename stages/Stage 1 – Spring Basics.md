# Stage 1 – Spring Basics

> **Estimated Duration:** 2-3 weeks  
> **Prerequisites:** Java 17+, Maven, IDE (IntelliJ IDEA recommended)

## Functional Requirements [FR]

* The system should provide full **CRUD** functionality for managing [domain models](../README.md#domain-models)
* It must run a **daily** automated job to identify courses starting the next day and send email notifications to all enrolled students using a dedicated thread pool
* The system needs to allow students to purchase courses using an internal currency (coins), verifying that they have a sufficient balance before the transaction

---

## Steps

### 1. [SPRING] Create Spring Boot Project Backbone

Create a new Spring Boot project with proper `pom.xml` configuration.

**Key Dependencies:**
```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
    
    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.6.3</version>
    </dependency>
</dependencies>
```

**📚 Documentation:**
- [Spring Initializr](https://start.spring.io/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/)

**✅ Verification:** Application starts without errors: `mvn spring-boot:run`

---

### 2. [DB] Setup Database Schema with Liquibase

Configure H2 in-memory database and create schema using Liquibase migrations.

**application.yml configuration:**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:lmsdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  h2:
    console:
      enabled: true
      path: /h2-console
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
```

**Liquibase master changelog:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <include file="db/changelog/db.changelog-create-schema.xml"/>
</databaseChangeLog>
```

**📚 Documentation:**
- [Liquibase Documentation](https://docs.liquibase.com/)
- [H2 Database](https://www.h2database.com/)

**💡 Tips:**
- Use UUIDs for primary keys for better scalability
- Create separate changelog files for each migration
- Add initial test data in a separate changelog

**✅ Verification:** Access H2 console at `http://localhost:8080/h2-console`

---

### 3. [DB] CRUD for Students and Courses

Implement full CRUD operations using JPA repositories with Lombok and MapStruct.

**Entity Example (Student):**
```java
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private Integer coins = 0;
    
    @ManyToMany
    @JoinTable(
        name = "enrollments",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> enrolledCourses = new ArrayList<>();
}
```

**Repository:**
```java
public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByEmail(String email);
    List<Student> findByEnrolledCoursesContaining(Course course);
}
```

**DTO Example:**
```java
@Data
@Builder
public class StudentDto {
    private UUID id;
    @NotBlank(message = "Name is required")
    private String name;
    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;
    private Integer coins;
}
```

**MapStruct Mapper:**
```java
@Mapper(componentModel = "spring")
public interface StudentMapper {
    StudentDto toDto(Student entity);
    Student toEntity(StudentDto dto);
    List<StudentDto> toDtoList(List<Student> entities);
}
```

**📚 Documentation:**
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/)
- [MapStruct](https://mapstruct.org/documentation/stable/reference/html/)
- [Lombok](https://projectlombok.org/features/)

**💡 Tips:**
- Use `@Builder` for cleaner object creation
- Keep DTOs separate from entities (never expose entities directly)
- Use meaningful method names in repositories (Spring Data will auto-generate queries)

---

### 4. [SPRING] Add Global Error Handler

Implement centralized exception handling with `@RestControllerAdvice`.

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
}
```

**📚 Documentation:**
- [Exception Handling in Spring](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)

---

### 5. [WEB] Enable Spring Boot Actuator

Configure health, info, and logging endpoints.

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**application.yml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,loggers,metrics
  endpoint:
    health:
      show-details: always
    loggers:
      enabled: true
  info:
    env:
      enabled: true

info:
  app:
    name: Learning Management System
    version: '@project.version@'
    description: Spring Boot LMS Application
```

**📚 Documentation:**
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/)

**✅ Verification:**
- Health: `GET /actuator/health`
- Info: `GET /actuator/info`
- Loggers: `GET /actuator/loggers`

---

### 6. [WEB] Add Swagger/OpenAPI Documentation

Configure Springdoc OpenAPI for REST API documentation.

**Dependency:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.0</version>
</dependency>
```

**Configuration:**
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Learning Management System API")
                .version("1.0.0")
                .description("REST API for LMS"));
    }
}
```

**📚 Documentation:**
- [Springdoc OpenAPI](https://springdoc.org/)

**✅ Verification:** Access Swagger UI at `http://localhost:8080/swagger-ui.html`

---

### 7. [WEB] Add Validation for DTOs

Use Jakarta Bean Validation annotations.

```java
@Data
public class CreateCourseDto {
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be 3-100 characters")
    private String title;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    private Integer price;
    
    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDate startDate;
}
```

**Controller with Validation:**
```java
@RestController
@RequestMapping("/api/v1/courses")
@Validated
public class CourseController {
    
    @PostMapping
    public ResponseEntity<CourseDto> create(@Valid @RequestBody CreateCourseDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(courseService.create(dto));
    }
}
```

**📚 Documentation:**
- [Bean Validation](https://jakarta.ee/specifications/bean-validation/)

---

### 8. [SPRING] Disable Open Session in View (OSIV)

**Why disable OSIV?**
- Keeps database connections open during view rendering
- Can hide N+1 problems
- May cause performance issues in production

**application.yml:**
```yaml
spring:
  jpa:
    open-in-view: false
```

**⚠️ Important:** After disabling OSIV, you'll need to:
- Use `@Transactional` properly on service methods
- Initialize lazy collections within transaction boundaries
- Use `JOIN FETCH` or `@EntityGraph` for related entities

**📚 Documentation:**
- [OSIV Anti-pattern](https://vladmihalcea.com/the-open-session-in-view-anti-pattern/)

---

### 9. [FR] Implement Daily Course Notification Job

Create a scheduled job to find courses starting tomorrow.

```java
@Service
@Slf4j
public class CourseStartNotificationScheduler {

    private final CourseRepository courseRepository;
    private final EmailService emailService;

    // Run daily at 8:00 AM
    @Scheduled(cron = "0 0 8 * * *")
    public void notifyStudentsAboutUpcomingCourses() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        List<Course> coursesStartingTomorrow = courseRepository
            .findByStartDate(tomorrow);
        
        log.info("Found {} courses starting tomorrow", coursesStartingTomorrow.size());
        
        for (Course course : coursesStartingTomorrow) {
            for (Student student : course.getEnrolledStudents()) {
                emailService.sendCourseStartNotification(student, course);
            }
        }
    }
}
```

**Enable Scheduling:**
```java
@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**📚 Documentation:**
- [Task Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)

---

### 10. [JOB] Create Custom Thread Pool for Email Sending

Configure async execution with custom thread pool.

```java
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Bean("emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

**Async Email Service:**
```java
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async("emailTaskExecutor")
    public void sendCourseStartNotification(Student student, Course course) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(student.getEmail());
            message.setSubject("Course Starting Tomorrow: " + course.getTitle());
            message.setText("Dear " + student.getName() + ",\n\n" +
                "Your course '" + course.getTitle() + "' starts tomorrow!");
            
            mailSender.send(message);
            log.info("Email sent to {}", student.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}", student.getEmail(), e);
        }
    }
}
```

**📚 Documentation:**
- [Async Processing](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async)

---

### 11. [FR] Implement Email Notification with Mailtrap

Configure SMTP settings for [Mailtrap](https://mailtrap.io/) testing.

**application.yml:**
```yaml
spring:
  mail:
    host: sandbox.smtp.mailtrap.io
    port: 2525
    username: ${MAILTRAP_USERNAME}
    password: ${MAILTRAP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**💡 Tips:**
- Use environment variables for credentials
- Mailtrap is great for development/testing
- Never commit real SMTP credentials

---

### 12. [FR] Implement Coin-Based Course Purchase

Implement transactional purchase logic with proper locking.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CoursePurchaseService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public PurchaseResult purchaseCourse(UUID studentId, UUID courseId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found"));
        
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException("Course not found"));
        
        // Check if already enrolled
        if (student.getEnrolledCourses().contains(course)) {
            throw new IllegalStateException("Already enrolled in this course");
        }
        
        // Check balance
        if (student.getCoins() < course.getPrice()) {
            throw new InsufficientBalanceException(
                "Not enough coins. Required: " + course.getPrice() + 
                ", Available: " + student.getCoins());
        }
        
        // Deduct coins and enroll
        student.setCoins(student.getCoins() - course.getPrice());
        student.getEnrolledCourses().add(course);
        
        studentRepository.save(student);
        
        log.info("Student {} purchased course {} for {} coins", 
            studentId, courseId, course.getPrice());
        
        return new PurchaseResult(true, "Course purchased successfully");
    }
}
```

**💡 Tips:**
- Use proper transaction isolation to prevent race conditions
- Consider optimistic locking with `@Version` for high-concurrency scenarios
- Always validate business rules before making changes

---

### 13. [TEST] Write Unit and Integration Tests

**Unit Test Example:**
```java
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    
    @Mock
    private CourseMapper courseMapper;
    
    @InjectMocks
    private CourseService courseService;

    @Test
    void shouldReturnCourseById() {
        // Given
        UUID id = UUID.randomUUID();
        Course course = Course.builder().id(id).title("Test").build();
        CourseDto dto = CourseDto.builder().id(id).title("Test").build();
        
        when(courseRepository.findById(id)).thenReturn(Optional.of(course));
        when(courseMapper.toDto(course)).thenReturn(dto);
        
        // When
        CourseDto result = courseService.findById(id);
        
        // Then
        assertThat(result.getTitle()).isEqualTo("Test");
        verify(courseRepository).findById(id);
    }
}
```

**Integration Test Example:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class CourseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateCourse() throws Exception {
        String json = """
            {
                "title": "Spring Boot Course",
                "description": "Learn Spring Boot",
                "price": 100
            }
            """;

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Spring Boot Course"));
    }
}
```

**📚 Documentation:**
- [Testing in Spring Boot](https://docs.spring.io/spring-boot/reference/testing/)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito](https://site.mockito.org/)

---

### 14. [VERSION] Create CHANGELOG.md

Document all changes following [Keep a Changelog](https://keepachangelog.com/) format.

```markdown
# Changelog

All notable changes to this project will be documented in this file.

## [0.1.0] - 2024-XX-XX

### Added
- Initial project setup with Spring Boot 3.x
- Student and Course entities with CRUD operations
- Liquibase database migrations
- Email notification system with custom thread pool
- Coin-based course purchase functionality
- Swagger/OpenAPI documentation
- Global exception handling
- Spring Boot Actuator endpoints
- Unit and integration tests

### Changed
- Disabled Open Session in View (OSIV)
```

---

## Achievements

### [SPRING]
| Concept | Description |
|---------|-------------|
| Spring Boot Actuator | Health checks, metrics, and monitoring |
| OSIV | Understanding and disabling Open Session in View |
| Swagger | REST API documentation with OpenAPI |
| Validation | Bean Validation for DTOs |
| @RestController | REST API endpoints |
| @RestControllerAdvice | Global exception handling |
| @ConfigurationProperties | Type-safe configuration |
| @Transactional | Declarative transaction management |
| @Async | Asynchronous method execution |
| @Lock | JPA locking strategies |
| @Scheduled | Cron-based task scheduling |

### [WEB]
| Concept | Description |
|---------|-------------|
| REST API Conventions | HTTP methods, status codes, resource naming |

### [DB]
| Concept | Description |
|---------|-------------|
| JPA/Hibernate | Object-relational mapping |
| Transaction Isolation | REPEATABLE_READ, SERIALIZABLE |
| Fetch Types | Lazy vs Eager loading |
| N+1 Problem | Query optimization awareness |
| Liquibase | Database version control |

### [TOOLING]
| Concept | Description |
|---------|-------------|
| MapStruct | Compile-time DTO mapping |
| Lombok | Boilerplate code reduction |

### [TEST]
| Concept | Description |
|---------|-------------|
| JUnit 5 | Unit testing framework |
| Mockito | Mocking framework |

### [VERSION]
| Concept | Description |
|---------|-------------|
| CHANGELOG.md | Change documentation |

---

## Useful Resources

- [Value Objects in Spring Boot](https://dev.to/kirekov/spring-boot-power-of-value-objects-1oah)
- [Logging Best Practices](https://www.papertrail.com/solution/tips/logging-in-java-best-practices-and-tips/)
- [Spring Boot Best Practices](https://www.baeldung.com/spring-boot-best-practices)
- [Clean Architecture with Spring Boot](https://www.baeldung.com/hexagonal-architecture-ddd-spring)

---

## Common Pitfalls

1. **Exposing entities directly** - Always use DTOs for API responses
2. **Missing @Transactional** - Ensure service methods are transactional
3. **N+1 queries** - Use `JOIN FETCH` or `@EntityGraph`
4. **Hardcoded credentials** - Use environment variables
5. **Not handling exceptions** - Use global exception handler
6. **Blocking async methods** - Don't call `.get()` on CompletableFuture in async flow