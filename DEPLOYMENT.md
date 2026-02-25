# SAP BTP Cloud Foundry Deployment Guide

This document provides instructions for deploying the Learning Management System application to SAP BTP Cloud Foundry.

## Deployment Options

This project supports two deployment methods:

| Method | Tool | Best For |
|--------|------|----------|
| **MTA Deployment** (Recommended) | `mbt` + `cf deploy` | Production deployments, CI/CD pipelines, automated service management |
| **CF Push** | `cf push` | Quick deployments, development/testing, manual service management |

## Prerequisites

1. **SAP BTP Trial Account**
   - Sign up at: https://account.hanatrial.ondemand.com/
   - Enable Cloud Foundry environment in your subaccount

2. **Cloud Foundry CLI**
   - Download from: https://docs.cloudfoundry.org/cf-cli/install-go-cli.html
   - Verify installation: `cf --version`

3. **Cloud MTA Build Tool (mbt)** - For MTA deployment
   - Install via npm: `npm install -g mbt`
   - Or download from: https://sap.github.io/cloud-mta-build-tool/
   - Verify installation: `mbt --version`

4. **MultiApps CF CLI Plugin** - For MTA deployment
   - Install: `cf install-plugin multiapps`
   - Verify: `cf plugins | grep multiapps`

5. **Java 17 and Maven**
   - Java 17 JDK installed
   - Maven 3.8+ installed

## Service Bindings

The application requires the following SAP BTP services:

| Service | Plan | Instance Name | Description |
|---------|------|---------------|-------------|
| HANA DB | hana-free | lms-hana-db | SAP HANA Cloud database |
| XSUAA | application | lms-xsuaa | OAuth2 authentication service (shared tenant-mode) |
| SaaS Registry | application | lms-saas-registry | Marketplace registration & subscription management |
| Application Logging | lite | lms-application-logging | Centralized logging service |
| Application Autoscaler | standard | lms-application-autoscaler | Auto-scaling based on metrics |
| Destination | lite | lms-destination | External connectivity management |
| Feature Flags | lite | lms-feature-flags | Feature toggle management |
| User-Provided | - | lms-smtp-credentials | SMTP server credentials |

### Applications

| Application | Type | Description |
|-------------|------|-------------|
| lms-approuter | Node.js | Entry point for all requests, tenant routing |
| learning-management-system | Java | Backend Spring Boot application |

---

## Multitenancy Configuration

The application supports SAP BTP multitenancy with provider/subscriber model.

### Security Configuration (xs-security.json)

```json
{
  "xsappname": "learning-management-system",
  "tenant-mode": "shared",
  "scopes": [
    { "name": "$XSAPPNAME.user", "description": "Basic user access" },
    { "name": "$XSAPPNAME.admin", "description": "Administrator access" },
    { "name": "$XSAPPNAME.Callback", "description": "SaaS Provisioning callbacks" }
  ],
  "role-templates": [
    { "name": "User", "scope-references": ["$XSAPPNAME.user"] },
    { "name": "Admin", "scope-references": ["$XSAPPNAME.user", "$XSAPPNAME.admin"] }
  ],
  "role-collections": [
    { "name": "LMS_User", "role-template-references": ["$XSAPPNAME.User"] },
    { "name": "LMS_Admin", "role-template-references": ["$XSAPPNAME.Admin"] }
  ]
}
```

### SaaS Provisioning Configuration (saas-provisioning.json)

```json
{
  "xsappname": "learning-management-system",
  "appName": "learning-management-system",
  "displayName": "Learning Management System",
  "description": "Multi-tenant learning management system",
  "category": "Education",
  "appUrls": {
    "getDependencies": "~{lms-api/url}/callback/v1.0/dependencies",
    "onSubscription": "~{lms-api/url}/callback/v1.0/tenants/{tenantId}"
  }
}
```

### Approuter Configuration (approuter/xs-app.json)

```json
{
  "welcomeFile": "/api/v1/application-info",
  "authenticationMethod": "route",
  "routes": [
    { "source": "^/api/(.*)$", "destination": "lms-backend", "authenticationType": "xsuaa" },
    { "source": "^/actuator/(.*)$", "destination": "lms-backend", "authenticationType": "none" }
  ]
}
```

### Role Assignment for Users

After deployment, assign role collections to users:

1. **Go to SAP BTP Cockpit** → Security → Trust Configuration
2. **Select your Identity Provider** (e.g., SAP ID Service)
3. **Click on a user** → Assign Role Collection
4. **Assign:**
   - `LMS_User` for standard API access
   - `LMS_Admin` for admin endpoint access

### Subscribing from Another Subaccount

1. **Create a subscriber subaccount** in SAP BTP
2. **Enable Cloud Foundry** in the subscriber subaccount
3. **Navigate to** Service Marketplace → Find "Learning Management System"
4. **Click Subscribe** → Subscription will call your callback endpoints
5. **Access the app** via tenant-specific URL:
   ```
   https://{subscriber-subdomain}-lms-approuter.cfapps.us10-001.hana.ondemand.com
   ```

---

## Option 1: MTA Deployment (Recommended)

MTA (Multi-Target Application) deployment is the recommended approach for SAP BTP applications. It provides:
- **Declarative deployment**: All services and configurations defined in `mta.yaml`
- **Automated service management**: Services are created/updated automatically
- **Blue-green deployment**: Zero-downtime deployments
- **Rollback support**: Easy rollback to previous versions
- **CI/CD integration**: Ideal for automated pipelines

### MTA Deployment Steps

#### Step 1: Login to Cloud Foundry

```bash
# Login to SAP BTP Cloud Foundry
cf login -a https://api.cf.us10-001.hana.ondemand.com

# Or use SSO
cf login -a https://api.cf.us10-001.hana.ondemand.com --sso
```

#### Step 2: Configure SMTP Credentials

Before deploying, update the SMTP credentials in `mta.yaml`:

```yaml
# In mta.yaml, find the lms-smtp-credentials resource and update:
- name: lms-smtp-credentials
  type: org.cloudfoundry.user-provided-service
  parameters:
    service-name: lms-smtp-credentials
    config:
      host: sandbox.smtp.mailtrap.io
      port: "2525"
      username: "YOUR_ACTUAL_USERNAME"    # <-- Update this
      password: "YOUR_ACTUAL_PASSWORD"    # <-- Update this
      from: no-reply@lms.example.com
```

> **Security Note**: For production, consider using environment variables or a secrets manager instead of hardcoding credentials.

#### Step 3: Build the Application

```bash
# Build the Java application
mvn clean package -P cloud -DskipTests

# The JAR file will be created at: target/learning-management-system-0.1.0.jar
```

#### Step 4: Build the MTA Archive

```bash
# Build the MTA archive (.mtar file)
mbt build

# This creates: mta_archives/learning-management-system_0.1.0.mtar
```

Alternatively, build with custom output directory:

```bash
mbt build --mtar learning-management-system.mtar --target ./
```

> **Note:** MTA build will automatically include the Approuter module and all service configurations.

#### Step 5: Deploy to Cloud Foundry

```bash
# Deploy the MTA archive
cf deploy mta_archives/learning-management-system_0.1.0.mtar

# Or if you specified a custom target:
cf deploy learning-management-system.mtar
```

#### Deployment Options

```bash
# Blue-green deployment (zero-downtime)
cf bg-deploy mta_archives/learning-management-system_0.1.0.mtar

# Deploy without starting the application
cf deploy mta_archives/learning-management-system_0.1.0.mtar --no-start

# Force delete of existing services (use with caution!)
cf deploy mta_archives/learning-management-system_0.1.0.mtar --delete-services

# Skip service updates (deploy app only)
cf deploy mta_archives/learning-management-system_0.1.0.mtar --skip-ownership-validation
```

#### Step 6: Verify Deployment

```bash
# Check MTA deployment status
cf mtas

# Check MTA operations
cf mta-ops

# Get details of deployed MTA
cf mta learning-management-system

# Check application status (both approuter and backend)
cf apps

# View recent logs
cf logs learning-management-system --recent
cf logs lms-approuter --recent
```

#### Step 7: Assign Role Collections

```bash
# Get XSUAA service key to find the xsappname
cf service-key lms-xsuaa lms-xsuaa-key

# Then in SAP BTP Cockpit:
# 1. Security → Trust Configuration → Your IDP
# 2. Assign LMS_User or LMS_Admin to users
```

### MTA Management Commands

```bash
# List all deployed MTAs
cf mtas

# Get MTA details
cf mta learning-management-system

# List MTA operations (deployments/undeployments)
cf mta-ops

# Abort a running MTA operation
cf mta-ops --abort <operation-id>

# Undeploy MTA (removes app and services)
cf undeploy learning-management-system --delete-services

# Undeploy MTA (keep services)
cf undeploy learning-management-system
```

### MTA Configuration Files

| File | Description |
|------|-------------|
| `mta.yaml` | MTA descriptor - defines modules, resources, and dependencies |
| `xs-security.json` | XSUAA security configuration (shared tenant-mode, scopes, roles) |
| `saas-provisioning.json` | SaaS Registry configuration for marketplace |
| `autoscaler-config.json` | Application autoscaler policy |
| `approuter/xs-app.json` | Approuter routing configuration |
| `approuter/package.json` | Approuter Node.js dependencies |

---

## Option 2: CF Push Deployment

Traditional Cloud Foundry deployment using `cf push` with `manifest.yaml`. Best for quick deployments and development/testing scenarios.

### CF Push Deployment Steps

### Step 1: Login to Cloud Foundry

```bash
# Login to SAP BTP Cloud Foundry
cf login -a https://api.cf.us10-001.hana.ondemand.com

# Or use SSO
cf login -a https://api.cf.us10-001.hana.ondemand.com --sso
```

### Step 2: Create Required Services

You can create services manually or use the provided script:

```bash
# Make the script executable
chmod +x cf-services.sh

# Run the service creation script
./cf-services.sh
```

Or create services manually:

```bash
# 1. HANA DB (hana-free plan for trial)
cf create-service hana-cloud hana-free lms-hana-db

# 2. XSUAA Service (OAuth2 with shared tenant mode)
cf create-service xsuaa application lms-xsuaa -c xs-security.json

# 3. SaaS Registry (Marketplace registration)
cf create-service saas-registry application lms-saas-registry -c saas-provisioning.json

# 4. Application Logging Service
cf create-service application-logs lite lms-application-logging

# 5. Application Autoscaler
cf create-service autoscaler standard lms-application-autoscaler

# 6. Destination Service
cf create-service destination lite lms-destination

# 7. Feature Flags Service
cf create-service feature-flags lite lms-feature-flags

# 8. User-Provided SMTP Credentials Service
cf create-user-provided-service lms-smtp-credentials -p '{
  "host": "sandbox.smtp.mailtrap.io",
  "port": "2525",
  "username": "your-mailtrap-username",
  "password": "your-mailtrap-password",
  "from": "no-reply@lms.example.com"
}'
```

### Step 3: Build the Application

```bash
# Build with cloud profile
mvn clean package -P cloud -DskipTests

# The JAR file will be created at: target/learning-management-system-0.1.0.jar
```

### Step 3.5: Install Approuter Dependencies

```bash
# Navigate to approuter directory and install dependencies
cd approuter
npm install
cd ..
```

### Step 4: Deploy to Cloud Foundry

```bash
# Deploy both applications using manifest.yaml
cf push

# Or deploy with specific options
cf push -f manifest.yaml
```

> **Note:** The manifest.yaml includes both the Approuter and Backend applications.

### Step 5: Configure SMTP Destination (Optional)

If you want to use the Destination Service for SMTP credentials instead of User-Provided Service:

1. **Create Destination in SAP BTP Cockpit:**
   - Navigate to your Subaccount → Connectivity → Destinations
   - Click "New Destination"
   - Configure the destination:

   | Property | Value |
   |----------|-------|
   | Name | `lms-smtp` |
   | Type | `MAIL` |
   | URL | `smtp://sandbox.smtp.mailtrap.io:2525` |
   | User | Your SMTP username |
   | Password | Your SMTP password |

   - Add Additional Properties:
     - `mail.smtp.host` = `sandbox.smtp.mailtrap.io`
     - `mail.smtp.port` = `2525`
     - `mail.from` = `no-reply@lms.example.com`

2. **Create Feature Flag to enable Destination-based SMTP:**
   - Use Postman collection "SAP BTP - Feature Flags Service" requests
   - Or via API:
   ```bash
   # Get OAuth token first, then create the flag
   curl -X POST "{{ff_uri}}/api/v2/flags" \
     -H "Authorization: Bearer {{ff_access_token}}" \
     -H "Content-Type: application/json" \
     -d '{
       "id": "use-destination-smtp",
       "description": "When enabled, SMTP credentials are retrieved from Destination Service",
       "variation": false,
       "variationType": "BOOLEAN",
       "directDelivery": true
     }'
   ```

3. **Toggle between SMTP sources:**
   - **Feature flag DISABLED (default)**: Uses User-Provided Service (`lms-smtp-credentials`)
   - **Feature flag ENABLED**: Uses Destination Service (`lms-smtp` destination)

### Step 6: Configure Application Autoscaler (Optional)

```bash
# Attach autoscaler policy
cf attach-autoscaling-policy learning-management-system autoscaler-config.json

# Verify policy
cf autoscaling-policy learning-management-system
```

### Step 7: Verify Deployment

```bash
# Check application status
cf apps

# View recent logs
cf logs learning-management-system --recent

# Stream logs in real-time
cf logs learning-management-system

# Check service bindings
cf services

# Get application URL
cf app learning-management-system
```

## Configuration Files

### manifest.yaml
- Defines application deployment settings
- Specifies service bindings
- Sets environment variables for cloud profile

### application.yml (cloud profile)
- HANA database configuration (auto-configured from VCAP_SERVICES)
- Cloud-specific logging settings
- Health check endpoints

### logback-spring.xml
- **Local (default/dev/prod)**: Plain text format with timestamp, thread, app name, version
- **Cloud**: JSON format for SAP Application Logging Service integration

## Logging Configuration

### Local Environment (Plain Text)
```
2026-02-11 16:30:00.123 [main] [learning-management-system] [0.1.0] INFO  c.l.m.Application - Application started
```

### Cloud Environment (JSON)
```json
{
  "timestamp": "2026-02-11T15:30:00.123Z",
  "level": "INFO",
  "thread": "main",
  "logger": "com.lms.mentoring.Application",
  "message": "Application started"
}
```

## Troubleshooting

### MTA Deployment Issues

1. **MTA build fails**
   ```bash
   # Check mbt version
   mbt --version
   
   # Validate mta.yaml syntax
   mbt validate
   
   # Build with verbose output
   mbt build -v
   ```

2. **MTA deployment fails**
   ```bash
   # Check MTA operation status
   cf mta-ops
   
   # Get detailed operation logs
   cf dmol -i <operation-id>
   
   # Retry failed deployment
   cf deploy mta_archives/learning-management-system_0.1.0.mtar --retries 3
   ```

3. **Service already exists (owned by another MTA)**
   ```bash
   # Check which MTA owns the service
   cf service <service-name>
   
   # Option 1: Undeploy the other MTA first
   cf undeploy <other-mta-id>
   
   # Option 2: Skip ownership validation (use existing services)
   cf deploy mta_archives/learning-management-system_0.1.0.mtar --skip-ownership-validation
   ```

4. **Blue-green deployment stuck**
   ```bash
   # List running operations
   cf mta-ops
   
   # Abort the operation
   cf mta-ops --abort <operation-id>
   
   # Clean up idle apps (after bg-deploy)
   cf delete learning-management-system-idle -f
   ```

### CF Push Issues

1. **Service creation fails**
   ```bash
   # Check available services and plans
   cf marketplace
   
   # Check service status
   cf service lms-hana-db
   ```

2. **Application fails to start**
   ```bash
   # Check staging logs
   cf logs learning-management-system --recent
   
   # Check events
   cf events learning-management-system
   ```

3. **Database connection issues**
   ```bash
   # Verify service binding
   cf env learning-management-system
   
   # Rebind service
   cf unbind-service learning-management-system lms-hana-db
   cf bind-service learning-management-system lms-hana-db
   cf restage learning-management-system
   ```

4. **Memory issues**
   ```bash
   # Scale application
   cf scale learning-management-system -m 2G
   
   # Or update manifest.yaml and redeploy
   ```

### Useful Commands

```bash
# View environment variables
cf env learning-management-system

# SSH into application container
cf ssh learning-management-system

# Restart application
cf restart learning-management-system

# Restage application (applies new service bindings)
cf restage learning-management-system

# Delete application
cf delete learning-management-system -f

# Delete all services
cf delete-service lms-hana-db -f
cf delete-service lms-application-logging -f
cf delete-service lms-application-autoscaler -f
cf delete-service lms-destination -f
cf delete-service lms-feature-flags -f
```

## Health Endpoints

The application exposes the following actuator endpoints:

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health status |
| `/actuator/health/liveness` | Kubernetes liveness probe |
| `/actuator/health/readiness` | Kubernetes readiness probe |
| `/actuator/info` | Application information |
| `/actuator/loggers` | Log level management |
| `/actuator/metrics` | Application metrics |

## Remote Debugging

You can enable remote debugging for the application deployed on SAP BTP Cloud Foundry.

### Enable Remote Debug

1. **Set debug environment variable:**
   ```bash
   cf set-env learning-management-system JBP_CONFIG_DEBUG '{ enabled: true }'
   cf restage learning-management-system
   ```

2. **Create SSH tunnel:**
   ```bash
   cf ssh -N -T -L 8000:localhost:8000 learning-management-system
   ```

3. **Configure IntelliJ IDEA:**
   - Go to Run → Edit Configurations
   - Add new "Remote JVM Debug" configuration
   - Set Host: `localhost`, Port: `8000`
   - Start debugging

4. **Disable when done:**
   ```bash
   cf unset-env learning-management-system JBP_CONFIG_DEBUG
   cf restage learning-management-system
   ```

## SMTP Configuration

The application supports two methods for retrieving SMTP credentials:

### Method 1: User-Provided Service (Default)

Credentials are stored in a Cloud Foundry user-provided service:

```bash
# Create or update SMTP credentials
cf create-user-provided-service lms-smtp-credentials -p '{
  "host": "sandbox.smtp.mailtrap.io",
  "port": "2525",
  "username": "your-username",
  "password": "your-password",
  "from": "no-reply@lms.example.com"
}'

# Update existing credentials
cf update-user-provided-service lms-smtp-credentials -p '{
  "host": "new-smtp-host.com",
  "port": "587",
  "username": "new-username",
  "password": "new-password",
  "from": "sender@example.com"
}'
```

### Method 2: Destination Service

Credentials are managed via SAP BTP Destination Service:

1. Create a destination named `lms-smtp` in SAP BTP Cockpit
2. Enable the feature flag `use-destination-smtp`
3. The application will automatically use Destination Service

### Switching Between Methods

Use the Feature Flags Service to switch at runtime:

```bash
# Get service credentials
cf env learning-management-system | grep feature-flags

# Use Postman collection or API to toggle the flag:
# - variation: false → Use User-Provided Service
# - variation: true → Use Destination Service
```

## Security Considerations

1. **VCAP_SERVICES**: Database credentials are automatically injected via Cloud Foundry service bindings
2. **No hardcoded secrets**: All sensitive configuration comes from environment variables
3. **HTTPS**: Cloud Foundry automatically provides HTTPS termination
4. **SMTP Credentials**: Stored securely in User-Provided Service or Destination Service
5. **Feature Flags**: Enable runtime configuration without code changes

## Postman Collection

The project includes a comprehensive Postman collection (`postman/LMS Mentoring API.postman_collection.json`) with:

- **LMS API Requests**: Students, Courses, Enrollments, Actuator endpoints
- **SAP BTP - Destination Service**: OAuth token, list/get destinations
- **SAP BTP - Feature Flags Service**: OAuth token, CRUD operations for feature flags

### Environment Files

- `postman/local.postman_environment.json` - For local development (`http://localhost:8080`)
- `postman/cloud.postman_environment.json` - For cloud deployment (includes SAP BTP service variables)

### Getting VCAP_SERVICES Credentials

To populate Postman environment variables:

```bash
# Get all environment variables
cf env learning-management-system

# Extract specific service credentials
cf env learning-management-system | grep -A 50 '"destination"'
cf env learning-management-system | grep -A 50 '"feature-flags"'
```

Copy the following values to your Postman cloud environment:
- `destination_clientid`, `destination_clientsecret`, `destination_token_url`, `destination_uri`
- `ff_clientid`, `ff_clientsecret`, `ff_token_url`, `ff_uri`

## Multitenancy Endpoints

The application exposes the following multitenancy-related endpoints:

| Endpoint | Method | Access | Description |
|----------|--------|--------|-------------|
| `/api/v1/application-info` | GET | ADMIN role | Returns XSUAA service credentials |
| `/callback/v1.0/dependencies` | GET | Callback scope | Returns service dependencies |
| `/callback/v1.0/tenants/{tenantId}` | PUT | Callback scope | Subscription callback |
| `/callback/v1.0/tenants/{tenantId}` | DELETE | Callback scope | Unsubscription callback |

### Testing Application Info Endpoint

```bash
# 1. Get OAuth token (user must have LMS_Admin role)
curl -X POST "https://{xsuaa-url}/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "{clientid}:{clientsecret}" \
  -d "grant_type=password&username={user}&password={pass}"

# 2. Call application-info endpoint
curl -X GET "https://lms-approuter.cfapps.us10-001.hana.ondemand.com/api/v1/application-info" \
  -H "Authorization: Bearer {access_token}"
```

---

## Additional Resources

### MTA Resources
- [Cloud MTA Build Tool (mbt)](https://sap.github.io/cloud-mta-build-tool/)
- [MTA Descriptor Schema](https://help.sap.com/docs/btp/sap-business-technology-platform/mta-descriptor-syntax)
- [MultiApps CF CLI Plugin](https://github.com/cloudfoundry/multiapps-cli-plugin)
- [MTA Deployment Guide](https://help.sap.com/docs/btp/sap-business-technology-platform/multitarget-application-deployment)

### SAP BTP Documentation
- [SAP BTP Documentation](https://help.sap.com/btp)
- [Cloud Foundry Documentation](https://docs.cloudfoundry.org/)
- [SAP HANA Cloud](https://help.sap.com/docs/hana-cloud)
- [Application Autoscaler](https://help.sap.com/docs/Application_Autoscaler)
- [Feature Flags Service](https://help.sap.com/docs/feature-flags-service)
- [Destination Service](https://help.sap.com/docs/connectivity/sap-btp-connectivity-cf/consuming-destination-service)
- [XSUAA Service](https://help.sap.com/docs/btp/sap-business-technology-platform/user-authentication-and-authorization)
