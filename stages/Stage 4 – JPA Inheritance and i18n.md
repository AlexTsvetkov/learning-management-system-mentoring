# Stage 4 – JPA Inheritance and i18n

> **Estimated Duration:** 2 weeks  
> **Prerequisites:** Completed Stage 3

## Functional Requirements [FR]

* Handle two distinct types of lessons: **ClassroomLesson** and **VideoLesson** using JPA inheritance
* Support **email localization** by adding a `locale` field to Student and using Mustache templates
* Implement **data auditing** with `created`, `createdBy`, `lastChanged`, `lastChangedBy` fields
* All **GET all** API requests must support **pagination**
* Resolve any **N+1 query** issues for efficient data retrieval
* Deploy using **MTA extension** file

---

## New DB Models

### ClassroomLesson
| Field     | Type    | Description |
|-----------|---------|-------------|
| location  | String  | Physical room/building location |
| capacity  | Integer | Maximum attendees |

### VideoLesson
| Field    | Type   | Description |
|----------|--------|-------------|
| url      | String | Video URL (YouTube, Vimeo, etc.) |
| platform | String | Platform name (e.g., YouTube, Zoom) |

---

## Steps

### 1. [DB] Implement JPA Inheritance for Lessons

**Base Entity with Single Table Inheritance:**
```java
@Entity
@Table(name = "lessons")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "lesson_type", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Integer duration; // in minutes
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;
}
```

**ClassroomLesson:**
```java
@Entity
@DiscriminatorValue("CLASSROOM")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ClassroomLesson extends Lesson {
    
    @Column
    private String location;
    
    @Column
    private Integer capacity;
}
```

**VideoLesson:**
```java
@Entity
@DiscriminatorValue("VIDEO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class VideoLesson extends Lesson {
    
    @Column
    private String url;
    
    @Column
    private String platform;
}
```

**Liquibase Changelog:**
```xml
<changeSet id="add-lesson-types" author="developer">
    <addColumn tableName="lessons">
        <column name="lesson_type" type="VARCHAR(50)" defaultValue="VIDEO">
            <constraints nullable="false"/>
        </column>
        <column name="location" type="VARCHAR(255)"/>
        <column name="capacity" type="INTEGER"/>
        <column name="url" type="VARCHAR(500)"/>
        <column name="platform" type="VARCHAR(100)"/>
    </addColumn>
</changeSet>
```

**📚 Documentation:**
- [JPA Inheritance Strategies](https://www.baeldung.com/hibernate-inheritance)
- [Lombok @SuperBuilder](https://projectlombok.org/features/experimental/SuperBuilder)

**💡 Tips:**
- `SINGLE_TABLE` is most efficient for polymorphic queries
- Use `JOINED` if tables have many type-specific columns
- Consider `TABLE_PER_CLASS` only if no polymorphic queries needed

---

### 2. [i18n] Add Locale Field to Student

**Update Student Entity:**
```java
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    // ... existing fields
    
    @Column(length = 10)
    private String locale = "en"; // Default to English
}
```

**Liquibase Migration:**
```xml
<changeSet id="add-student-locale" author="developer">
    <addColumn tableName="students">
        <column name="locale" type="VARCHAR(10)" defaultValue="en">
            <constraints nullable="false"/>
        </column>
    </addColumn>
</changeSet>
```

**Supported Locales:**
```java
public enum SupportedLocale {
    ENGLISH("en"),
    GERMAN("de"),
    RUSSIAN("ru");
    
    private final String code;
}
```

---

### 3. [i18n] Create Email Templates with Mustache

**Dependencies:**
```xml
<dependency>
    <groupId>com.github.spullara.mustache.java</groupId>
    <artifactId>compiler</artifactId>
    <version>0.9.14</version>
</dependency>
```

**Template Structure:**
```
src/main/resources/templates/email/
├── course-start-notification_en.mustache
├── course-start-notification_de.mustache
├── course-start-notification_ru.mustache
├── course-start-subject_en.mustache
├── course-start-subject_de.mustache
└── course-start-subject_ru.mustache
```

**English Template (course-start-notification_en.mustache):**
```html
Dear {{studentName}},

Your course "{{courseTitle}}" starts tomorrow!

Course Details:
- Start Date: {{startDate}}
- Duration: {{duration}} hours
- Instructor: {{instructor}}

Best regards,
Learning Management System
```

**German Template (course-start-notification_de.mustache):**
```html
Sehr geehrte(r) {{studentName}},

Ihr Kurs "{{courseTitle}}" beginnt morgen!

Kursdetails:
- Startdatum: {{startDate}}
- Dauer: {{duration}} Stunden
- Dozent: {{instructor}}

Mit freundlichen Grüßen,
Learning Management System
```

**Email Template Service:**
```java
@Service
@Slf4j
public class EmailTemplateService {

    private final MustacheFactory mustacheFactory;

    public EmailTemplateService() {
        this.mustacheFactory = new DefaultMustacheFactory("templates/email");
    }

    public String renderTemplate(String templateName, String locale, Map<String, Object> context) {
        String templateFile = templateName + "_" + locale + ".mustache";
        
        try {
            Mustache mustache = mustacheFactory.compile(templateFile);
            StringWriter writer = new StringWriter();
            mustache.execute(writer, context).flush();
            return writer.toString();
        } catch (Exception e) {
            log.warn("Template not found for locale {}, falling back to English", locale);
            return renderTemplate(templateName, "en", context);
        }
    }
    
    public String renderSubject(String templateName, String locale, Map<String, Object> context) {
        return renderTemplate(templateName + "-subject", locale, context);
    }
}
```

**📚 Documentation:**
- [Mustache.java](https://github.com/spullara/mustache.java)
- [Mustache Manual](https://mustache.github.io/mustache.5.html)

---

### 4. [AUDIT] Implement JPA Auditing

**Enable JPA Auditing:**
```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // Get current user from SecurityContext
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of("system");
            }
            return Optional.of(auth.getName());
        };
    }
}
```

**Abstract Auditable Entity:**
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
public abstract class AuditableEntity {
    
    @CreatedDate
    @Column(name = "created", nullable = false, updatable = false)
    private Instant created;
    
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;
    
    @LastModifiedDate
    @Column(name = "last_changed", nullable = false)
    private Instant lastChanged;
    
    @LastModifiedBy
    @Column(name = "last_changed_by", nullable = false)
    private String lastChangedBy;
}
```

**Extend Entities:**
```java
@Entity
@Table(name = "courses")
public class Course extends AuditableEntity {
    // ... existing fields
}
```

**Liquibase Migration:**
```xml
<changeSet id="add-audit-fields" author="developer">
    <addColumn tableName="courses">
        <column name="created" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="created_by" type="VARCHAR(100)" defaultValue="system">
            <constraints nullable="false"/>
        </column>
        <column name="last_changed" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="last_changed_by" type="VARCHAR(100)" defaultValue="system">
            <constraints nullable="false"/>
        </column>
    </addColumn>
</changeSet>
```

**📚 Documentation:**
- [Spring Data JPA Auditing](https://docs.spring.io/spring-data/jpa/reference/auditing.html)

---

### 5. [DB] Resolve N+1 Query Issues with @EntityGraph

**Problem - N+1 Query:**
```java
// This causes N+1 problem!
List<Course> courses = courseRepository.findAll();
for (Course course : courses) {
    course.getLessons().size(); // Each access triggers a query
}
```

**Solution 1 - @EntityGraph:**
```java
public interface CourseRepository extends JpaRepository<Course, UUID> {
    
    @EntityGraph(attributePaths = {"lessons", "enrolledStudents"})
    List<Course> findAllWithLessonsAndStudents();
    
    @EntityGraph(attributePaths = {"lessons"})
    Optional<Course> findWithLessonsById(UUID id);
}
```

**Solution 2 - JPQL JOIN FETCH:**
```java
public interface CourseRepository extends JpaRepository<Course, UUID> {
    
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.lessons")
    List<Course> findAllWithLessons();
    
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.lessons WHERE c.id = :id")
    Optional<Course> findByIdWithLessons(@Param("id") UUID id);
}
```

**Solution 3 - Batch Fetching:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 20
```

**💡 Tips:**
- Use Hibernate query logging to detect N+1: `spring.jpa.show-sql=true`
- Consider using [p6spy](https://github.com/p6spy/p6spy) for query analysis
- Use `@BatchSize` annotation on collections

---

### 6. [WEB] Implement Pagination for GET All Endpoints

**Controller with Pagination:**
```java
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<Page<CourseDto>> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<CourseDto> courses = courseService.findAll(pageable);
        
        return ResponseEntity.ok(courses);
    }
}
```

**Service with Pagination:**
```java
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public Page<CourseDto> findAll(Pageable pageable) {
        return courseRepository.findAll(pageable)
            .map(courseMapper::toDto);
    }
}
```

**Repository with EntityGraph and Pagination:**
```java
public interface CourseRepository extends JpaRepository<Course, UUID> {
    
    @EntityGraph(attributePaths = {"lessons"})
    Page<Course> findAll(Pageable pageable);
}
```

**API Response Example:**
```json
{
  "content": [
    {"id": "...", "title": "Spring Boot", ...},
    {"id": "...", "title": "Java Advanced", ...}
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {"sorted": true, "empty": false}
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false,
  "first": true,
  "empty": false
}
```

**📚 Documentation:**
- [Spring Data Pagination](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.limit-query-result)

---

### 7-10. [TEST] Configure Test Structure and Maven Plugins

**Test Directory Structure:**
```
src/test/java/com/lms/mentoring/
├── unit/
│   ├── course/
│   │   ├── controller/CourseControllerTest.java
│   │   ├── service/CourseServiceTest.java
│   │   └── mapper/CourseMapperTest.java
│   └── student/
│       └── ...
└── integration/
    ├── CourseRepositoryIT.java
    └── StudentRepositoryIT.java
```

**Surefire Plugin (Unit Tests):**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.4</version>
    <configuration>
        <includes>
            <include>**/unit/**/*Test.java</include>
        </includes>
        <excludes>
            <exclude>**/*IT.java</exclude>
            <exclude>**/integration/**/*.java</exclude>
        </excludes>
        <argLine>${surefireArgLine}</argLine>
    </configuration>
</plugin>
```

**Failsafe Plugin (Integration Tests):**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.5.4</version>
    <configuration>
        <includes>
            <include>**/*IT.java</include>
            <include>**/integration/**/*Test.java</include>
        </includes>
        <argLine>${failsafeArgLine}</argLine>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**JaCoCo Plugin (Coverage Reports):**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <!-- Unit tests coverage -->
        <execution>
            <id>pre-unit-test</id>
            <goals><goal>prepare-agent</goal></goals>
            <configuration>
                <destFile>${project.build.directory}/coverage-reports/jacoco-ut.exec</destFile>
                <propertyName>surefireArgLine</propertyName>
            </configuration>
        </execution>
        <execution>
            <id>post-unit-test</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/coverage-reports/jacoco-ut.exec</dataFile>
                <outputDirectory>${project.reporting.outputDirectory}/jacoco-ut</outputDirectory>
            </configuration>
        </execution>
        <!-- Integration tests coverage -->
        <execution>
            <id>pre-integration-test</id>
            <phase>pre-integration-test</phase>
            <goals><goal>prepare-agent</goal></goals>
            <configuration>
                <destFile>${project.build.directory}/coverage-reports/jacoco-it.exec</destFile>
                <propertyName>failsafeArgLine</propertyName>
            </configuration>
        </execution>
        <execution>
            <id>post-integration-test</id>
            <phase>post-integration-test</phase>
            <goals><goal>report</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/coverage-reports/jacoco-it.exec</dataFile>
                <outputDirectory>${project.reporting.outputDirectory}/jacoco-it</outputDirectory>
            </configuration>
        </execution>
        <!-- Merged report -->
        <execution>
            <id>merge-results</id>
            <phase>verify</phase>
            <goals><goal>merge</goal></goals>
            <configuration>
                <fileSets>
                    <fileSet>
                        <directory>${project.build.directory}/coverage-reports</directory>
                        <includes><include>*.exec</include></includes>
                    </fileSet>
                </fileSets>
                <destFile>${project.build.directory}/coverage-reports/jacoco-merged.exec</destFile>
            </configuration>
        </execution>
        <execution>
            <id>merged-report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/coverage-reports/jacoco-merged.exec</dataFile>
                <outputDirectory>${project.reporting.outputDirectory}/jacoco-merged</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Run Tests:**
```bash
# Unit tests only
mvn test

# Integration tests only
mvn failsafe:integration-test

# All tests with coverage
mvn verify

# View reports
open target/site/jacoco-merged/index.html
```

**📚 Documentation:**
- [Maven Surefire](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [Maven Failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/)
- [JaCoCo](https://www.jacoco.org/jacoco/)

---

### 11. [CF] Create MTA Extension File

**mta-extension.mtaext:**
```yaml
_schema-version: "3.2"
ID: learning-management-system-ext
extends: learning-management-system

modules:
  - name: learning-management-system
    properties:
      # Override SMTP credentials for production
      SMTP_HOST: "production.smtp.server.com"
      SMTP_PORT: "587"

resources:
  - name: lms-smtp-credentials
    parameters:
      config:
        host: "production.smtp.server.com"
        port: "587"
        username: "prod-username"
        password: "prod-password"
        from: "noreply@production.com"
```

**Deploy with Extension:**
```bash
# Build
mbt build

# Deploy with extension
cf deploy mta_archives/learning-management-system_0.1.0.mtar -e mta-extension.mtaext
```

**📚 Documentation:**
- [MTA Extension Descriptors](https://help.sap.com/docs/btp/sap-business-technology-platform/defining-mta-extension-descriptors)

---

## Achievements

### [DB]
| Concept | Description |
|---------|-------------|
| JPA Inheritance | SINGLE_TABLE, JOINED, TABLE_PER_CLASS strategies |
| @EntityGraph | Fetch plan optimization |
| N+1 Resolution | JOIN FETCH, batch fetching |

### [i18n]
| Concept | Description |
|---------|-------------|
| Mustache Templates | Logic-less templating |
| Locale-based Content | Language-specific email templates |

### [AUDIT]
| Concept | Description |
|---------|-------------|
| @EnableJpaAuditing | Enable audit features |
| @EntityListeners | AuditingEntityListener |
| @CreatedDate/@LastModifiedDate | Automatic timestamps |
| @CreatedBy/@LastModifiedBy | User tracking |
| SecurityContextHolder | Current user resolution |

### [WEB]
| Concept | Description |
|---------|-------------|
| Pageable | Pagination interface |
| Page | Paginated result wrapper |

### [TEST]
| Concept | Description |
|---------|-------------|
| Surefire | Unit test execution |
| Failsafe | Integration test execution |
| JaCoCo | Code coverage analysis |
| @Tag | Test categorization |

### [CF]
| Concept | Description |
|---------|-------------|
| MTA Extension | Environment-specific overrides |

---

## Common Pitfalls

1. **Inheritance mapping** - Choose strategy based on query patterns
2. **N+1 in pagination** - Use `@EntityGraph` with paginated queries
3. **Audit on new entities** - Ensure `@CreatedDate` fields are not null
4. **Template not found** - Implement fallback to default locale
5. **Merge report fails** - Ensure both unit and integration tests run
6. **Extension file syntax** - Use correct `extends` reference