# Stage 2 – SAP BTP Basics

> **Estimated Duration:** 2-3 weeks  
> **Prerequisites:** Completed Stage 1, SAP BTP Trial Account

## Functional Requirements [FR]

* The application must be deployable to SAP BTP, where it will bind to several essential services, including a **HANA DB**, an **Application Logging Service**, and a **Destination Service**
* The system must retrieve SMTP server credentials for sending emails, with the retrieval method controlled by a **Feature Flag**:
  - If the flag is **enabled** → get credentials from the **Destination Service**
  - If the flag is **disabled** → use a **User-provided** service
* The system must use **caching** and a **retry mechanism** for all external API calls
* All application logs should be formatted in **plain text** locally and encoded as **JSON** for the cloud environment

---

## System Architecture Overview

![stage2-flow-diagram.png](../img/stage2-flow-diagram.png)

---

## Steps

### 1. [CF] Configure manifest.yaml and Cloud Profile

Create deployment configuration for Cloud Foundry.

**manifest.yaml:**
```yaml
---
applications:
  - name: learning-management-system
    memory: 1024M
    disk_quota: 512M
    instances: 1
    buildpack: sap_java_buildpack_jakarta
    path: target/learning-management-system-0.1.0.jar
    routes:
      - route: learning-management-system.cfapps.us10-001.hana.ondemand.com
    env:
      JBP_CONFIG_OPEN_JDK_JRE: '{ jre: { version: 17.+ } }'
      JBP_CONFIG_COMPONENTS: '{ jres: ["com.sap.xs.java.buildpack.jre.SAPMachineJRE"] }'
      SPRING_PROFILES_ACTIVE: cloud
    services:
      - lms-hana-db
      - lms-xsuaa
      - lms-destination
      - lms-feature-flags
      - lms-application-logging
      - lms-application-autoscaler
```

**application-cloud.yml:**
```yaml
spring:
  profiles: cloud
  datasource:
    driver-class-name: com.sap.db.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.HANAColumnStoreDialect
    hibernate:
      ddl-auto: none

# Disable local mail config in cloud
  mail:
    host: ""
```

**📚 Documentation:**
- [Cloud Foundry Manifest](https://docs.cloudfoundry.org/devguide/deploy-apps/manifest.html)
- [SAP Java Buildpack](https://help.sap.com/docs/btp/sap-business-technology-platform/java-buildpacks)

---

### 2. [CF] Create SAP BTP Trial Account and Deploy

**Step-by-step deployment:**

1. **Create SAP BTP Trial Account:**
   - Visit [SAP BTP Trial](https://account.hanatrial.ondemand.com/)
   - Follow registration process
   - Access your trial subaccount

2. **Install Cloud Foundry CLI:**
   ```bash
   # macOS
   brew install cloudfoundry/tap/cf-cli@8
   
   # Verify installation
   cf --version
   ```

3. **Login to Cloud Foundry:**
   ```bash
   # Get API endpoint from BTP Cockpit > Subaccount > Overview
   cf login -a https://api.cf.us10-001.hana.ondemand.com
   
   # Or use SSO
   cf login -a https://api.cf.us10-001.hana.ondemand.com --sso
   ```

4. **Build and Deploy:**
   ```bash
   # Build with cloud profile
   mvn clean package -P cloud -DskipTests
   
   # Deploy
   cf push
   ```

**📚 Documentation:**
- [SAP BTP Trial](https://developers.sap.com/tutorials/hcp-create-trial-account.html)
- [CF CLI Reference](https://cli.cloudfoundry.org/en-US/v8/)

**✅ Verification:**
```bash
cf apps                    # List applications
cf logs lms --recent      # View recent logs
cf env lms                # View environment variables
```

---

### 3. [CF] Bind Cloud Services

Create and bind required SAP BTP services.

**Create Services:**
```bash
# HANA Cloud Database (use hana-free for trial)
cf create-service hana-cloud hana-free lms-hana-db

# Application Logging
cf create-service application-logs lite lms-application-logging

# Application Autoscaler
cf create-service autoscaler standard lms-application-autoscaler

# Destination Service
cf create-service destination lite lms-destination

# Feature Flags Service
cf create-service feature-flags lite lms-feature-flags
```

**⚠️ Note:** HANA Cloud instance creation may take 15-30 minutes. Check status:
```bash
cf service lms-hana-db
```

**Bind Services (if not in manifest):**
```bash
cf bind-service learning-management-system lms-hana-db
cf bind-service learning-management-system lms-application-logging
# ... etc
```

**View VCAP_SERVICES:**
```bash
cf env learning-management-system
```

**📚 Documentation:**
- [SAP HANA Cloud](https://help.sap.com/docs/hana-cloud)
- [Application Logging](https://help.sap.com/docs/application-logging-service)
- [Destination Service](https://help.sap.com/docs/connectivity/sap-btp-connectivity-cf/consuming-destination-service)
- [Feature Flags](https://help.sap.com/docs/feature-flags-service)
- [Application Autoscaler](https://help.sap.com/docs/application-autoscaler)

---

### 4. [LOG] Configure Plain Text Logging (Local)

**logback-spring.xml for local profile:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="dev,dev-postgres">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{app.name}:%X{app.version}] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
        
        <logger name="com.lms.mentoring" level="DEBUG"/>
    </springProfile>
</configuration>
```

**Add MDC values in application:**
```java
@Component
public class LoggingMdcFilter implements Filter {
    
    @Value("${spring.application.name}")
    private String appName;
    
    @Value("${info.app.version:unknown}")
    private String appVersion;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        MDC.put("app.name", appName);
        MDC.put("app.version", appVersion);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

---

### 5. [LOG] Configure JSON Logging (Cloud)

**logback-spring.xml for cloud profile:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Cloud profile with JSON encoding -->
    <springProfile name="cloud">
        <appender name="STDOUT-JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
                <layout class="ch.qos.logback.contrib.json.classic.JsonLayout">
                    <timestampFormat>yyyy-MM-dd'T'HH:mm:ss.SSSZ</timestampFormat>
                    <appendLineSeparator>true</appendLineSeparator>
                    <jsonFormatter class="ch.qos.logback.contrib.jackson.JacksonJsonFormatter">
                        <prettyPrint>false</prettyPrint>
                    </jsonFormatter>
                </layout>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="STDOUT-JSON"/>
        </root>
    </springProfile>
</configuration>
```

**Dependencies:**
```xml
<dependency>
    <groupId>ch.qos.logback.contrib</groupId>
    <artifactId>logback-json-classic</artifactId>
    <version>0.1.5</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback.contrib</groupId>
    <artifactId>logback-jackson</artifactId>
    <version>0.1.5</version>
</dependency>
```

**📚 Documentation:**
- [SAP Application Logging](https://help.sap.com/docs/application-logging-service)
- [Logback JSON Encoder](https://github.com/qos-ch/logback-contrib)

---

### 6. [FR] Create User-Provided Service with SMTP Credentials

Create a user-provided service for SMTP configuration (fallback method).

```bash
# Create user-provided service with SMTP credentials
cf create-user-provided-service lms-smtp-credentials -p '{
  "host": "sandbox.smtp.mailtrap.io",
  "port": "2525",
  "username": "your-mailtrap-username",
  "password": "your-mailtrap-password",
  "from": "no-reply@lms.example.com"
}'

# Bind to application
cf bind-service learning-management-system lms-smtp-credentials

# Restage application
cf restage learning-management-system
```

**Read credentials from VCAP_SERVICES:**
```java
@Component
@Slf4j
public class VcapSmtpCredentialsProvider implements SmtpCredentialsProvider {

    @Override
    public SmtpCredentials getCredentials() {
        String vcapServices = System.getenv("VCAP_SERVICES");
        if (vcapServices == null) {
            throw new SmtpCredentialsException("VCAP_SERVICES not available");
        }
        
        try {
            JsonNode root = new ObjectMapper().readTree(vcapServices);
            JsonNode userProvided = root.get("user-provided");
            
            for (JsonNode service : userProvided) {
                if ("lms-smtp-credentials".equals(service.get("name").asText())) {
                    JsonNode creds = service.get("credentials");
                    return SmtpCredentials.builder()
                        .host(creds.get("host").asText())
                        .port(Integer.parseInt(creds.get("port").asText()))
                        .username(creds.get("username").asText())
                        .password(creds.get("password").asText())
                        .from(creds.get("from").asText())
                        .build();
                }
            }
            throw new SmtpCredentialsException("SMTP credentials not found in VCAP_SERVICES");
        } catch (Exception e) {
            throw new SmtpCredentialsException("Failed to parse VCAP_SERVICES", e);
        }
    }
}
```

**📚 Documentation:**
- [User-Provided Services](https://docs.cloudfoundry.org/devguide/services/user-provided.html)

---

### 7. [FR] Create Destination with SMTP Credentials

Create a destination in SAP BTP Cockpit.

**Steps:**
1. Go to **BTP Cockpit** → **Subaccount** → **Connectivity** → **Destinations**
2. Click **New Destination**
3. Configure:
   ```
   Name: lms-smtp
   Type: HTTP (we'll use it for configuration, not actual HTTP calls)
   URL: https://sandbox.smtp.mailtrap.io
   Authentication: NoAuthentication
   
   Additional Properties:
   - mail.smtp.host = sandbox.smtp.mailtrap.io
   - mail.smtp.port = 2525
   - mail.smtp.username = your-username
   - mail.smtp.password = your-password
   - mail.from = no-reply@lms.example.com
   ```

**📚 Documentation:**
- [Destination Service](https://help.sap.com/docs/connectivity/sap-btp-connectivity-cf/http-destinations)

---

### 8. [FR] Implement Destination Service Integration

Retrieve SMTP credentials via Destination Service API.

**Dependencies (SAP Cloud SDK):**
```xml
<dependency>
    <groupId>com.sap.cloud.sdk.cloudplatform</groupId>
    <artifactId>cloudplatform-connectivity</artifactId>
    <version>5.18.0</version>
</dependency>
<dependency>
    <groupId>com.sap.cloud.sdk.cloudplatform</groupId>
    <artifactId>connectivity-destination-service</artifactId>
    <version>5.18.0</version>
</dependency>
```

**Implementation:**
```java
@Component
@Slf4j
public class DestinationSmtpCredentialsProvider implements SmtpCredentialsProvider {

    private static final String DESTINATION_NAME = "lms-smtp";

    @Override
    public SmtpCredentials getCredentials() {
        try {
            Destination destination = DestinationAccessor
                .getDestination(DESTINATION_NAME);
            
            Map<String, String> props = destination.getPropertyNames().stream()
                .collect(Collectors.toMap(
                    name -> name,
                    name -> destination.get(name).orElse("")
                ));
            
            return SmtpCredentials.builder()
                .host(props.get("mail.smtp.host"))
                .port(Integer.parseInt(props.get("mail.smtp.port")))
                .username(props.get("mail.smtp.username"))
                .password(props.get("mail.smtp.password"))
                .from(props.get("mail.from"))
                .build();
                
        } catch (Exception e) {
            log.error("Failed to get SMTP credentials from Destination Service", e);
            throw new SmtpCredentialsException("Destination Service error", e);
        }
    }
}
```

**📚 Documentation:**
- [SAP Cloud SDK for Java](https://sap.github.io/cloud-sdk/docs/java/overview)
- [Destination Service API](https://help.sap.com/docs/connectivity/sap-btp-connectivity-cf/consuming-destination-service)

---

### 9. [FR] Implement VCAP-based SMTP Provider

*Already covered in Step 6*

---

### 10. [FR] Implement Feature Flag-Based Strategy Switching

Use Feature Flags Service to control SMTP credential source.

**Create Feature Flag in BTP Cockpit:**
1. Go to **Feature Flags Service** dashboard
2. Create flag: `use-destination-smtp`
3. Set initial state: `disabled`

**Feature Flags Configuration:**
```java
@Configuration
@ConfigurationProperties(prefix = "feature-flags")
@Data
public class FeatureFlagsConfig {
    private String uri;
    private String username;
    private String password;
}
```

**Feature Flags Service:**
```java
@Service
@Slf4j
public class FeatureFlagsService {

    private final RestTemplate restTemplate;
    private final FeatureFlagsConfig config;

    public boolean isEnabled(String flagName) {
        try {
            String url = config.getUri() + "/api/v1/evaluate/" + flagName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(config.getUsername(), config.getPassword());
            
            ResponseEntity<FeatureFlagResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FeatureFlagResponse.class
            );
            
            return response.getBody() != null && 
                   Boolean.TRUE.equals(response.getBody().isEnabled());
        } catch (Exception e) {
            log.warn("Failed to evaluate feature flag: {}", flagName, e);
            return false;
        }
    }
}
```

**Strategy Resolver:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpCredentialsResolver {

    private final FeatureFlagsService featureFlagsService;
    private final DestinationSmtpCredentialsProvider destinationProvider;
    private final VcapSmtpCredentialsProvider vcapProvider;

    public SmtpCredentials resolve() {
        boolean useDestination = featureFlagsService.isEnabled("use-destination-smtp");
        
        if (useDestination) {
            log.info("Using Destination Service for SMTP credentials");
            return destinationProvider.getCredentials();
        } else {
            log.info("Using User-Provided Service for SMTP credentials");
            return vcapProvider.getCredentials();
        }
    }
}
```

**📚 Documentation:**
- [Feature Flags Service API](https://help.sap.com/docs/feature-flags-service/feature-flags-service/api)

---

### 11. [FR] Add Cache and Retry for External API Calls

**Enable caching and retry:**
```java
@SpringBootApplication
@EnableCaching
@EnableRetry
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

**Cache Configuration:**
```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(100));
        return cacheManager;
    }
}
```

**Usage with @Cacheable and @Retryable:**
```java
@Service
@Slf4j
public class DestinationSmtpCredentialsProvider implements SmtpCredentialsProvider {

    @Override
    @Cacheable(value = "smtp-credentials", unless = "#result == null")
    @Retryable(
        value = {RestClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public SmtpCredentials getCredentials() {
        log.info("Fetching SMTP credentials from Destination Service");
        // ... implementation
    }
    
    @Recover
    public SmtpCredentials recoverCredentials(RestClientException e) {
        log.error("All retry attempts failed", e);
        throw new SmtpCredentialsException("Failed to fetch credentials after retries", e);
    }
}
```

**📚 Documentation:**
- [Spring Cache](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Retry](https://github.com/spring-projects/spring-retry)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)

---

### 12. [DEBUG] Enable Remote Debugging

**In manifest.yaml:**
```yaml
env:
  JBP_CONFIG_DEBUG: '{ enabled: true }'
```

**Connect from IntelliJ IDEA:**
1. Enable SSH tunnel:
   ```bash
   cf enable-ssh learning-management-system
   cf restage learning-management-system
   cf ssh -N -L 8000:localhost:8000 learning-management-system
   ```

2. Create Remote Debug configuration in IntelliJ:
   - **Run** → **Edit Configurations** → **+** → **Remote JVM Debug**
   - Host: `localhost`
   - Port: `8000`
   - Command line arguments: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000`

3. Start debugging session

**📚 Documentation:**
- [Remote Debugging on SAP BTP](https://help.sap.com/docs/btp/sap-business-technology-platform/java-options)

---

### 13-14. [TOOLING] Prepare Postman Collection

Create comprehensive Postman collection for testing.

**Collection Structure:**
```
LMS API
├── Authentication
│   └── Get OAuth Token (XSUAA)
├── Students
│   ├── Get All Students
│   ├── Get Student by ID
│   ├── Create Student
│   ├── Update Student
│   └── Delete Student
├── Courses
│   ├── Get All Courses
│   ├── Get Course by ID
│   ├── Create Course
│   ├── Update Course
│   └── Delete Course
├── Purchases
│   └── Purchase Course
├── External Services
│   ├── Destination Service
│   │   ├── Get OAuth Token
│   │   ├── List Destinations
│   │   └── Get Destination by Name
│   └── Feature Flags Service
│       ├── List Flags
│       └── Evaluate Flag
└── Health
    └── Actuator Health
```

**Environment Variables:**
```json
{
  "host": "https://your-app.cfapps.us10-001.hana.ondemand.com",
  "xsuaa_url": "https://your-subdomain.authentication.us10.hana.ondemand.com",
  "xsuaa_clientid": "sb-your-app!t12345",
  "xsuaa_clientsecret": "xxx",
  "destination_url": "https://destination-configuration.cfapps.us10.hana.ondemand.com",
  "destination_clientid": "sb-clone...",
  "destination_clientsecret": "xxx",
  "ff_uri": "https://feature-flags.cfapps.us10.hana.ondemand.com",
  "ff_username": "sbss_xxx",
  "ff_password": "xxx"
}
```

**OAuth Token Request Script:**
```javascript
// Pre-request script for OAuth token
pm.sendRequest({
    url: pm.environment.get("xsuaa_url") + "/oauth/token",
    method: 'POST',
    header: {
        'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: {
        mode: 'urlencoded',
        urlencoded: [
            {key: 'grant_type', value: 'client_credentials'},
            {key: 'client_id', value: pm.environment.get("xsuaa_clientid")},
            {key: 'client_secret', value: pm.environment.get("xsuaa_clientsecret")}
        ]
    }
}, function(err, res) {
    pm.environment.set("access_token", res.json().access_token);
});
```

**Export collection:** Save as `postman/LMS Mentoring API.postman_collection.json`

---

## Achievements

### [CF]
| Concept | Description |
|---------|-------------|
| manifest.yaml | CF application deployment configuration |
| Manual Deployment | `cf push` command usage |
| HANA DB | SAP HANA Cloud database binding |
| Application Logging | Centralized log management |
| Application Autoscaler | Auto-scaling based on metrics |
| Destination Service | External connectivity management |
| Feature Flags Service | Feature toggle management |
| User-provided Service | Custom service configuration |

### [DEBUG]
| Concept | Description |
|---------|-------------|
| Remote Debugging | SSH tunnel + IDE debug configuration |

### [TOOLING]
| Concept | Description |
|---------|-------------|
| Postman | API testing and documentation |

### [AUTH]
| Concept | Description |
|---------|-------------|
| RestTemplate/RestClient | HTTP client for API calls |
| OAuth2 Integration | Client credentials flow |
| Basic Auth Integration | Username/password authentication |

### [SPRING]
| Concept | Description |
|---------|-------------|
| @Profile | Environment-specific configuration |
| @Cacheable | Method-level caching |
| @Retryable | Automatic retry mechanism |

---

## Useful Commands

```bash
# Deployment
cf push                              # Deploy application
cf restage <app-name>               # Restage application

# Services
cf services                          # List bound services
cf service <service-name>           # Service details
cf env <app-name>                   # Environment variables

# Logs
cf logs <app-name>                  # Stream logs
cf logs <app-name> --recent         # Recent logs

# SSH
cf ssh <app-name>                   # SSH into container
cf enable-ssh <app-name>            # Enable SSH access

# Scaling
cf scale <app-name> -i 2            # Scale to 2 instances
cf scale <app-name> -m 2G           # Set memory to 2GB
```

---

## Common Pitfalls

1. **HANA Cloud not ready** - Wait for service creation to complete (can take 15-30 min)
2. **Missing VCAP_SERVICES** - Ensure services are bound and app is restaged
3. **OAuth token expiry** - Implement token refresh logic
4. **Cache invalidation** - Consider cache eviction strategies
5. **Feature flag defaults** - Handle cases when FF service is unavailable
6. **Log format in tests** - Disable JSON logging in test profile