# Stage 2 – SAP BTP Basics

# Functional requirement [FR]

* The application must be deployable to SAP BTP, where it will bind to several essential services, including a **HANA DB**, 
an **Application Logging Service**, and a **Destination Service**. 
* The system must retrieve SMTP server credentials for sending emails, with the retrieval method controlled by a **Feature Flag**. 
Specifically, if the flag is enabled, it must get credentials from the **Destination Service**, but if disabled, it must use a **User-provided** service. 
* The system must use caching and a retry mechanism for all external API calls. 
* All application logs should be formatted in plain text locally and encoded as **JSON** for the cloud environment.

## Result system overview

![stage2-flow-diagram.png](../img/stage2-flow-diagram.png)


## Steps

1. **[CF]** Configure `manifest.yaml` for application deployment and create `cloud` profile
2. **[CF]** Create SAP BTP trial account and deploy application to `SAP BTP`
3. **[CF]** Bind 
   * HANA DB (schema plan)
   * Application Logging Service
   * Application Autoscaler
   * Destination Service
   * Feature Flags Service
4. **[LOG]** Update logs with plain text format locally (add timestamp, thread-name, app-name, app-version values)  
5. **[LOG]** Update logs with JSON encoding format for a cloud environment  
6. **[FR]** Create `User-provided service` with SMTP server credentials
7. **[FR]** Create Destination with `SMTP server` credentials
8. **[FR]** Implement logic for retrieving `SMTP server` credentials via `Destination Service API`
9. **[FR]** Implement logic for retrieving `SMTP server` credentials via `User provided VCAP`
10. **[FR]** Implement logic for switching `SMTP server` credentials retrieval strategy based on Feature Flag value: 
    * **Disabled** – get credentials from User-provided service 
    * **Enabled** – get credentials from Destination Service 
11. **[FR]** Add Cache and Retry for external API calls
12. **[DEBUG]** Enable remote debug from IDE
13. **[TOOLING]** Prepare Postman requests for Destination and Feature Flags services APIs
14. **[TOOLING]** Export and store Postman collection with requests in the project

# Achievements

#### [**CF**]

1. `manifest.yaml` config
2. Manual deployment of app to the `BTP` via `CLI`
3. `Hana DB`
4. Application Cloud Logging  
5. Application Autoscaler
6. Destination Service
7. Feature Flags Service  
8. User-provided service  

#### [**DEBUG**]
1. Remote debugging

#### [**TOOLING**]
1. Postman

#### [**AUTH**]
1. RestTemplate/RestClient  
2. Integration with API with OAuth2  
3. Integration with API with Basic Auth

#### [**SPRING**]
1. @Profile
2. @Cacheable
3. @Retryable
