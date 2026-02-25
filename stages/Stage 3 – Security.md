# Stage 3 – Security

> **Estimated Duration:** 1-2 weeks  
> **Prerequisites:** Completed Stage 2, Docker installed

## Functional Requirements [FR]

* The application's API endpoints must be accessible only to **authenticated users**
* Authentication varies by environment:
  - **Locally:** Basic Auth
  - **Cloud:** XSUAA service with OAuth2
* **Spring Actuator** endpoints are restricted to users with "**MANAGER**" role (Basic Auth in both environments)
* The system must use **PostgreSQL** in Docker for local development (replacing H2)
* The application must be deployed using **mta.yaml** instead of manifest.yaml

---

## Steps

### 1. [SECURITY] Store User Details in Memory for Basic Auth

**WebSecurityConfig for Local:**
```java
@Configuration
@EnableWebSecurity
@Profile({"dev", "dev-postgres"})
public class LocalSecurityConfig {

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
        
        UserDetails manager = User.builder()
            .username("manager")
            .password(passwordEncoder().encode("manager123"))
            .roles("USER", "MANAGER")
            .build();
        
        return new InMemoryUserDetailsManager(user, manager);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**📚 Documentation:**
- [Spring Security](https://docs.spring.io/spring-security/reference/)

---

### 2. [SECURITY] Secure API Endpoints Locally (Basic Auth)

**Security Filter Chain:**
```java
@Configuration
@EnableWebSecurity
@Profile({"dev", "dev-postgres"})
public class LocalSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Swagger UI - permit all
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // H2 Console
                .requestMatchers("/h2-console/**").permitAll()
                // Actuator - MANAGER role only
                .requestMatchers("/actuator/**").hasRole("MANAGER")
                // API endpoints - authenticated users
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults())
            .headers(headers -> headers.frameOptions(f -> f.disable())); // H2 Console
        
        return http.build();
    }
}
```

**Test with curl:**
```bash
# Without auth - 401 Unauthorized
curl http://localhost:8080/api/v1/students

# With Basic Auth
curl -u user:password http://localhost:8080/api/v1/students

# Actuator - requires MANAGER role
curl -u manager:manager123 http://localhost:8080/actuator/health
```

---

### 3. [SECURITY] Configure Actuator Security (MANAGER Role)

**Already included in Step 2 configuration:**
```java
.requestMatchers("/actuator/**").hasRole("MANAGER")
```

**💡 Tips:**
- Keep actuator credentials separate from API credentials
- Consider using a dedicated actuator user

---

### 4. [SECURITY] Secure Cloud Endpoints with XSUAA (OAuth2)

**Dependencies:**
```xml
<dependency>
    <groupId>com.sap.cloud.security.xsuaa</groupId>
    <artifactId>spring-xsuaa</artifactId>
    <version>3.1.2</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

**xs-security.json (XSUAA configuration):**
```json
{
  "xsappname": "learning-management-system",
  "tenant-mode": "dedicated",
  "scopes": [
    {
      "name": "$XSAPPNAME.user",
      "description": "User scope"
    },
    {
      "name": "$XSAPPNAME.manager",
      "description": "Manager scope"
    }
  ],
  "role-templates": [
    {
      "name": "User",
      "description": "User role",
      "scope-references": ["$XSAPPNAME.user"]
    },
    {
      "name": "Manager",
      "description": "Manager role",
      "scope-references": ["$XSAPPNAME.user", "$XSAPPNAME.manager"]
    }
  ]
}
```

**Cloud Security Config:**
```java
@Configuration
@EnableWebSecurity
@Profile("cloud")
public class CloudSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Swagger UI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Health endpoint for load balancer
                .requestMatchers("/actuator/health").permitAll()
                // Other actuator endpoints - Basic Auth with MANAGER role
                .requestMatchers("/actuator/**").hasRole("MANAGER")
                // API endpoints - OAuth2 authenticated
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract scopes from XSUAA token
            List<String> scopes = jwt.getClaimAsStringList("scope");
            if (scopes == null) return Collections.emptyList();
            
            return scopes.stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toList());
        });
        return converter;
    }
}
```

**📚 Documentation:**
- [SAP XSUAA](https://help.sap.com/docs/btp/sap-business-technology-platform/what-is-user-account-and-authentication-service)
- [Spring OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)

---

### 5. [SECURITY] Actuator Basic Auth in Cloud

**Create separate security config for actuator in cloud:**
```java
@Configuration
@EnableWebSecurity
@Profile("cloud")
@Order(1)  // Higher priority for actuator
public class ActuatorSecurityConfig {

    @Value("${actuator.username}")
    private String username;
    
    @Value("${actuator.password}")
    private String password;

    @Bean
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().hasRole("MANAGER")
            )
            .httpBasic(Customizer.withDefaults());
        
        return http.build();
    }

    @Bean
    public UserDetailsService actuatorUserDetailsService() {
        UserDetails manager = User.builder()
            .username(username)
            .password(new BCryptPasswordEncoder().encode(password))
            .roles("MANAGER")
            .build();
        return new InMemoryUserDetailsManager(manager);
    }
}
```

**application-cloud.yml:**
```yaml
actuator:
  username: ${ACTUATOR_USERNAME:admin}
  password: ${ACTUATOR_PASSWORD:admin123}
```

---

### 6. [DB] Run PostgreSQL in Docker

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    container_name: lms-postgres
    environment:
      POSTGRES_DB: lmsdb
      POSTGRES_USER: lms
      POSTGRES_PASSWORD: lms123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U lms -d lmsdb"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres-data:
```

**Start PostgreSQL:**
```bash
docker-compose up -d
```

**application-dev-postgres.yml:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lmsdb
    username: lms
    password: lms123
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
```

**Dependencies:**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Run with PostgreSQL:**
```bash
mvn spring-boot:run -Dspring.profiles.active=dev-postgres
```

**📚 Documentation:**
- [Docker Compose](https://docs.docker.com/compose/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)

---

### 7. [CF] Replace manifest.yaml with mta.yaml

**mta.yaml (Multi-Target Application descriptor):**
```yaml
_schema-version: "3.2"
ID: learning-management-system
version: 0.1.0
description: Learning Management System - Spring Boot application

parameters:
  enable-parallel-deployments: true

build-parameters:
  before-all:
    - builder: custom
      commands:
        - echo "Run 'mvn clean package -P cloud -DskipTests' before 'mbt build'"

modules:
  - name: learning-management-system
    type: java
    path: .
    build-parameters:
      builder: custom
      commands: []
      build-result: target/learning-management-system-0.1.0.jar
    parameters:
      memory: 1024M
      disk-quota: 512M
      instances: 1
      buildpack: sap_java_buildpack_jakarta
      health-check-type: http
      health-check-http-endpoint: /actuator/health
    properties:
      JBP_CONFIG_OPEN_JDK_JRE: '{ jre: { version: 17.+ } }'
      JBP_CONFIG_COMPONENTS: '{ jres: ["com.sap.xs.java.buildpack.jre.SAPMachineJRE"] }'
      SPRING_PROFILES_ACTIVE: cloud
    requires:
      - name: lms-hana-db
      - name: lms-xsuaa
      - name: lms-destination
      - name: lms-feature-flags
      - name: lms-application-logging
      - name: lms-application-autoscaler
      - name: lms-smtp-credentials

resources:
  - name: lms-hana-db
    type: org.cloudfoundry.managed-service
    parameters:
      service: hana-cloud
      service-plan: hana-free
      service-name: lms-hana-db

  - name: lms-xsuaa
    type: org.cloudfoundry.managed-service
    parameters:
      service: xsuaa
      service-plan: application
      service-name: lms-xsuaa
      path: xs-security.json

  - name: lms-destination
    type: org.cloudfoundry.managed-service
    parameters:
      service: destination
      service-plan: lite
      service-name: lms-destination

  - name: lms-feature-flags
    type: org.cloudfoundry.managed-service
    parameters:
      service: feature-flags
      service-plan: lite
      service-name: lms-feature-flags

  - name: lms-application-logging
    type: org.cloudfoundry.managed-service
    parameters:
      service: application-logs
      service-plan: lite
      service-name: lms-application-logging

  - name: lms-application-autoscaler
    type: org.cloudfoundry.managed-service
    parameters:
      service: autoscaler
      service-plan: standard
      service-name: lms-application-autoscaler

  - name: lms-smtp-credentials
    type: org.cloudfoundry.user-provided-service
    parameters:
      service-name: lms-smtp-credentials
      config:
        host: sandbox.smtp.mailtrap.io
        port: "2525"
        username: "your-username"
        password: "your-password"
        from: no-reply@lms.example.com
```

**Build and Deploy with MTA:**
```bash
# Install MBT (MTA Build Tool)
npm install -g mbt

# Build Java application
mvn clean package -P cloud -DskipTests

# Build MTA archive
mbt build

# Deploy
cf deploy mta_archives/learning-management-system_0.1.0.mtar
```

**📚 Documentation:**
- [MTA Documentation](https://help.sap.com/docs/btp/sap-business-technology-platform/multitarget-applications-in-cloud-foundry-environment)
- [MBT Tool](https://sap.github.io/cloud-mta-build-tool/)

---

## Achievements

### [SECURITY]
| Concept | Description |
|---------|-------------|
| Security Configuration | Spring Security setup |
| Basic Auth | Username/password authentication |
| OAuth2 Configuration | JWT token validation |
| XSUAA Service | SAP identity management |

### [DB]
| Concept | Description |
|---------|-------------|
| Docker | Container runtime |
| PostgreSQL | Production-grade database |

### [CF]
| Concept | Description |
|---------|-------------|
| mta.yaml | Multi-Target Application descriptor |
| MBT | MTA Build Tool |

---

## Common Pitfalls

1. **Order matters** - Use `@Order` for multiple security filter chains
2. **CSRF disabled** - Appropriate for stateless REST APIs
3. **Role naming** - Spring adds "ROLE_" prefix automatically
4. **Token validation** - Ensure XSUAA binding is correct
5. **H2 console security** - Disable frame options for console access
6. **Database migrations** - Ensure Liquibase scripts work with PostgreSQL

---

## Testing Security

**Get OAuth Token (Cloud):**
```bash
# Get client credentials from VCAP_SERVICES
CLIENT_ID="sb-learning-management-system!t12345"
CLIENT_SECRET="xxx"
XSUAA_URL="https://your-subdomain.authentication.us10.hana.ondemand.com"

# Request token
curl -X POST "$XSUAA_URL/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET"

# Use token
curl -H "Authorization: Bearer <token>" \
  https://your-app.cfapps.us10-001.hana.ondemand.com/api/v1/students