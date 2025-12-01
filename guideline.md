# Comprehensive Code Review Guideline Stage 1

This document is a set of rules and recommendations based on previous reviews, aimed at maintaining high quality, readability, and performance of code.

---

## 1. Architecture and Design

* **Value Objects (VO):** Avoid "Primitive Obsession" ([Primitive Obsession](https://refactoring.guru/smells/primitive-obsession)). Instead of `String email` or `BigDecimal price`, use `ValueObject` classes (for example, `record Email(String value)`).
* **DRY (Don't Repeat Yourself):** Don't duplicate logic. If you need to retrieve an entity, use an existing service method (for example, `userService.findById(id)`), rather than accessing the repository (`userRepository.findById(id)`) directly from another class.
* **Use interfaces (Strategy Pattern):** If you anticipate that an implementation (such as payment method, delivery method) may change or there will be multiple implementations, use interfaces.
* **Repository Isolation:** A repository (for example, `StudentRepository`) should be injected **only** into its "domain" service (`StudentService`). Other services (for example, `CourseService`) should not access `StudentRepository` directly.
* **Immutability:** Prefer immutable objects (`record`, `final` fields). Creating new objects instead of modifying old ones makes code thread-safe by default.
* **Validation:** Complex business validation should be moved to separate validator classes, rather than mixing it with business logic in services.
* **Code Nesting**: Avoid deep nesting (Nesting Hell). If you see 3-4 levels of `if/for` inside a method, it's a sign of poor design. Extract nested logic into separate, well-named method.
* **Entry Point**: The main application class (with the `@SpringBootApplication` annotation) should be "clean" and contain only the `main` method.
* **Configuration Files (@Configuration)**:
  * Configuration beans should not be located in the main application class.
  * Each configuration should be in its own file (for example, `SwaggerConfiguration`, `SecurityConfiguration`).
  * Configuration files should be located in the appropriate packages (...config.swagger or ...security).
* **Idempotency**: Idempotent requests are safer than their regular counterparts; consider this when developing logic.

---

## 2. API, DTO, and `record`

* **Correct HTTP Statuses:** Use the right response codes.
  * **201 Created:** For successful resource creation (`POST`).
  * **204 No Content:** For successful deletion (`DELETE`) when there's nothing to return in the body.
  * **200 OK:** For successful `GET` and `PUT`/`PATCH`.
* **DTO Separation:** Clearly separate DTOs for requests and responses (`RequestDto` / `ResponseDto`). If necessary, there can be multiple RequestDto or ResponseDto.
* **API Granularity:** Endpoints should be focused.
  * `GET /api/v1/students/{id}` should not return the student's courses.
  * For related data, there should be a separate endpoint: `GET /api/v1/students/{id}/courses`.
  * For performing actions, use separate endpoints (`POST /api/v1/enrollments` - if represented as an entity / `POST /api/v1/students/{studentId}/courses/{courseId}`), rather than overloading `PUT /api/v1/students/{id}`.
* **Use `record`:** For DTOs, Value Objects, and other "data carriers," use `record` instead of `class`.
* **Mapping (MapStruct):** Use MapStruct for mapping (Entity <-> DTO). MapStruct can inject other mappers or Spring beans (`@Context`, `@Autowired`) for complex logic.

---

## 3. Performance and Asynchronicity

* **[Bulkhead Pattern](https://www.systemdesignacademy.com/blog/bulkhead-pattern)** (Isolation of Thread Pools and other resources):
  * Never use the default Spring thread pool (task executor) for `@Async` or scheduler.
  * Always create **separate thread pools** (Thread Pools) for different tasks (for example, one for jobs, one for asynchronous API requests). This prevents "cascading failures" when one slow task "eats up" all threads.
  * Use `@Async("mySpecificThreadPool")` along with `@Scheduled` to specify which [pool](https://habr.com/ru/articles/771112/) the task should run in.
* **N+1 Problem:** Make sure the `N+1` problem doesn't occur. All related data that will be used should, if possible, be loaded from the database in a single query (for example, via `JOIN FETCH` or `@EntityGraph`).
  * **Bad:** `list.stream().map(id -> repository.findById(id)).toList()`
  * **Good:** `repository.findAllByIds(listOfIds)`
* **Race Conditions:** Consider race conditions. When performing critical operations (course enrollment, payment, inventory management), make sure you use protective mechanisms (pessimistic/optimistic locks, transactions with the appropriate isolation level).

---

## 4. Testing

* **Test Naming:** Use the convention `[UnitOfWork_StateUnderTest_ExpectedBehavior]`.
  * **Example:** `enrollStudent_WhenCourseIsFull_ShouldThrowException`
* **Structure (Given-When-Then):** Clearly divide the test into three blocks using comments.
    ```java
    // given
    var student = TestDataGenerator.createDefaultStudent();
    var course = TestDataGenerator.createFullCourse();
    
    // when
    Result result = service.enrollStudent(student, course);
    
    // then
    assertFalse(result.isSuccess());
    // ...
    ```
* **Data Generators:** Don't create test data "manually" in each test. Use utility generator classes (for example, `TestDataGenerator`) to create valid entities.
* **Context Cleanup:** If tests use a shared database or Spring Context, use `@BeforeEach` or `@AfterEach` to clean up the state (for example, via `repository.deleteAll()`), so tests don't affect each other.

---

## 5. Database

* **`@ManyToMany` is rarely used in real life:** Instead of `@ManyToMany`, it's often necessary to create an explicit "Join entity" (for example, `Enrollment`) with two `@ManyToOne` relationships. This allows adding additional fields to the junction table (status, flag, audit).
* **Transaction Size:** Transactions (`@Transactional`) should be as "short" as possible and used only where necessary (usually on service methods that modify data).
* **Transaction Rollback:** Remember that by default, Spring does **not** roll back transactions for **checked exceptions**.
* **Migrations (Liquibase/Flyway):** In migration files, the `author` field should contain the developer's real email or identifier.

---

## 6. Error Handling and Logging

* **Exception Hierarchy:** Use custom, semantic exceptions.
  * Create a base exception (for example, `LmsException`).
  * Use nested static classes for specific errors: `StudentException.NotFound`.
  * **Example:** `throw new StudentException.NotFound(id);`
* **Error Localization:** When throwing an exception, pass an error code (`"student.not.found"`) and parameters (ID), not a ready-made message.
* **Logging Levels:**
  * `DEBUG`: Debugging information (method entry/exit).
  * `INFO`: Important business events (User X registered).
  * `WARN`: Non-critical errors.
  * `ERROR`: Exceptions that broke business logic.
* **Log Format:** Dynamic data (IDs, names) should always be at the end of the message. This simplifies searching in ElasticSearch.
  * **Good:** `Student not found by id: {}`, `id`

---

## 7. Naming

* **Meaningful Class Names:** Names should accurately reflect responsibility.
  * **Bad:** `ApplicationAspect`, `MainService`.
  * **Good:** `LoggingAspect`, `UserRegistrationService`.
* **Dependency Naming:** Inside a class, a dependency can be named by its role.
  * In `StudentService`: `private final StudentRepository repository;`
  * In `StudentController`: `private final StudentService service;`
* **Self-Documenting Code:** If the conjunction **"and"** appears in a method name (for example, `findUserAndValidate`), split it into two methods.
* **Collections:** Collection names should be plural (`List<User> users`).
* **DTOs:** Suffixes should follow CamelCase (`UserDto`, not `UserDTO`).

---

## 8. Java and Code Style

* **Constants:** Should be `static final` fields.
* **Using `var`:** Use `var` when the type is obvious from the right side (`final var list = new ArrayList<String>();`).
    ```java
        final List<String> list = new ArrayList<String>(); // Wrong (Redundant)
        final List<String> list = new ArrayList<>(); // Better, but discouraged
        final var list = new ArrayList<String>(); // Preferred
    ```
* **Annotation (jspecify):** For improved static analysis and explicit contract specification (for example, `@NonNull`, `@Nullable`), it's good practice to use annotations from the `jspecify` library.
* **Annotation Aggregation:** Use the minimum number of annotations. If a set of annotations (for example, for DTOs or controllers) is frequently repeated, create a new custom "aggregating" annotation.
* **Method Parameters:** A method should not contain many parameters. The maximum number is **three**. If there are more parameters, it's necessary to either split the method into several or create a parameter object (Parameter Object) to encapsulate them.
* **Modern `switch`:** Use only modern `switch` expressions (Java 14+) with the `->` syntax, as they are safer and more readable.
* **Pattern Matching `instanceof`:** Use "Pattern-matching `instanceof`" (type checks with immediate casting) for cleaner and safer code.
    ```java
    // Bad
    if (obj instanceof String) {
        String s = (String) obj;
        // ...
    }
    // Good
    if (obj instanceof String s) {
        // ...
    }
    ```
* **String Handling:**
  * **Text Blocks:** For multi-line text (SQL, JSON, XML), **always** use text blocks (Java 15+) instead of string concatenation.
  * **Formatting:** Prefer the `"...".formatted()` method (Java 15+) over `String.format()`.
* **Raw Types:** It is **forbidden** to use raw types. Generics should always be parameterized (for example, `List<String>`, not `List`). If parameterization is impossible, a comment explaining the reason should be provided.
* **Using [`Lombok @Builder`](https://projectlombok.org/features/Builder)`:** Use `builder` instead of constructor whenever possible.
    ```java
        // Wrong
        final var order = new Order("John Smith", "TestStreet 42", "FakeStreet 1", "Time Machine", 1, 1337.0f, false);
        final var updatedOrder = new Order(
                order.getName(),
                order.getShippingAddress(),
                order.getBillingAddress(),
                order.getProductName(),
                order.getQuantity(),
                order.getPrice(),
                true);
        
        // Correct
        final var order = Order.builder()
                .customerName("John Smith")
                .shippingAddress("TestStreet 42")
                .billingAddress("FakeStreet 1")
                .productName("Time Machine")
                .quantity(1)
                .price(1337.0f)
                .isExpressShipping(false)
                .build();
        final var updatedOrder = order.toBuilder().isExpressShipping(true).build();
    ```
* **`null` Checks and `Optional`:**
  * **Checks:** Use `Objects.nonNull(obj)`, `Objects.isNull(obj)`, and `CollectionUtils.isEmpty(list)` (Apache utils).
      ```java
          obj != null -> nonNull(obj)
          list != null && !list.isEmpty() -> CollectionUtils.isEmpty(list) 
      ```
  * **Returning `null`:** Avoid returning `null` from methods. Use `Optional` to represent an absent value.
  * **`Optional` in arguments:** It is **forbidden** to use `Optional` as a method argument. In such cases, use method overloading (one with a parameter, one without) or perform the check *before* calling the method.
* **Functional Style (Streams & Lambdas):**
  * **Lambda Size:** The preferred size of a lambda is **one line**. If the logic is more complex, it should be extracted into a separate private method (Method Reference).
  * **Side Effects:** Streams should not be used if the operation in them causes "side effects" (changing the state of external objects).
  * **Preference:** Prefer streams for multi-step, functional-style processing.
  * **Returning Stream:** In some cases (for example, in repositories), it may be more convenient and efficient to return a `Stream` instead of a ready-made collection.
* **Libraries:** `vavr` is a useful library for writing code in a functional style (for example, `Try` for exception handling, `Either` for returning a result or error).
* **Imports:**
  * Static imports (`import static...`) are preferred except in cases where it's better to explicitly indicate which package the dependency is from.
  * Wildcard imports (`*`) are forbidden. **Configure your IDE!**

---

## 9. Readability and Formatting

* **Don't Skimp on Variables:** Avoid long chains of calls. Use intermediate variables — this simplifies debugging and code reading.
  * **Bad:** `return mapper.toResponseDTO(repository.save(mapper.toEntity(studentDTO)));`
  * **Good:**
      ```java
          var studentFromDto = mapper.toEntity(studentDTO);
          var studentSaved = repository.save(studentFromDto);
          return mapper.toResponseDTO(studentSaved);
      ```
* **Line Length:** Maximum line length is **120 characters**.
* **End of File:** All `.java` files should end with one empty line (newline).
* **Logical Blocks**: Separate logical blocks in code (groups of fields in a class, stages in a method) with an empty line.
    ```java
        Example of fields in a record:
    
        @NonNull UUID id,
    
        @NonNull Instant createdAt,
        @NonNull String createdBy,
        @Nullable Instant updatedAt,
    
        @NonNull String firstName,
        @NonNull String lastName
    ```
* **Order**:
  * Lists (for example, in @Import) can be sorted alphabetically for easier searching.
      ```java
      @Import({
          AddressBookService.class,
          BtpFeatureFlagsService.class,
          Сache...
          ...
      })
      ```
  * Annotations can be sorted by meaning (for example, @Slf4j at the top) or by length (from longest to shortest).
      ```java
      @Slf4j
      @Aspect
      @Component
      ```
---

## 10. Configuration and Security

* **Format:** It's preferable to use `application.yml` (hierarchical, but sensitive to indentation) instead of `application.properties`.
* **Security:** It is **forbidden** to store passwords, API keys, and other secrets in `application.yml`. Use environment variables (`${DB_PASSWORD}`).
* **Scheduling:** `cron` expressions for `@Scheduled` should be moved to `application.yml`.
  * **Good:** `@Scheduled(cron = "${course.reminder.cron}")`

---

## 11. Project Management

* **Absence of Unused Code:**
  * There should be no commented-out lines or "dead" (uncalled) methods in the code.
  * There should be no unused dependencies in `pom.xml`.
  * `.gitignore` should only contain rules relevant to the project.
* **`pom.xml` Configuration:**
  * **Metadata:** `<description>`, `<groupId>`, `<artifactId>` should be correct (not `org.example`).
  * **Project Version**: The initial version of the project should be 1.0.0
  * **Dependency Versions:** All versions should be moved to constants in the `<properties>` section.
  * **Cleanliness**: There should be no empty, generated blocks (`<licenses>, <developers>, <scm>`).
  * **Dependency Grouping**: Dependencies in the `<dependencies>` section should be logically grouped using comments `<!-- Web--> <!-- Mail --> <!-- DB -->` and separated by an empty line for readability.