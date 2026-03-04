# Stage 3 – Security

# Functional requirement [FR]

* The application's API endpoints must be accessible only to **authenticated users**. 
* This authentication will vary by environment: _locally_, it will use `Basic Auth`, while in the _cloud_, it will be secured by the `XSUAA` service with `OAuth2`. 
* The `Spring Actuator` endpoints will be specifically restricted to users with the "**MANAGER**" role in both _local_ and _cloud_ environments, also using `Basic Auth`. 
* The system must use a `PostgreSQL` database running in a Docker container for local development, replacing the `H2 in-memory` database. 
* The application must be deployed to the cloud using an `mta.yaml` file.

## Steps

1. [**SECURITY**] Store user details in memory for `Basic Auth` flow  
2. [**SECURITY**] Secure API endpoints *locally* and allow access for **all authenticated** users (`Basic Auth`)  
3. [**SECURITY**] Configure security for Spring Actuator endpoints *locally* and allow access for **MANAGER** role only (`Basic Auth`)  
4. [**SECURITY**] Secure API endpoints in *cloud* using XSUAA service and allow access for **all authenticated** users (`OAuth2`)
5. [**SECURITY**] Configure security for Spring Actuator endpoints in *cloud* (`Basic Auth`) and allow access for **MANAGER** role only  
6. [**DB**] Run `PostgreSQL` in Docker and switch to it locally (**H2 is removed**)  
7. [**CF**] Replace `manifest.yaml` with `mta.yaml` and redeploy application

## **Achievements**

#### [SECURITY] 

1. Security configuration  
2. Basic Auth configuration  
3. OAuth2 configuration  
4. XSUAA service  

#### [DB] 

1. Docker 
2. PostgreSQL

#### [CF] 

1. `mta.yaml`