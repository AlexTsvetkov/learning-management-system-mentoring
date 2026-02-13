# SAP BTP Cloud Foundry Deployment Guide

This document provides instructions for deploying the Learning Management System application to SAP BTP Cloud Foundry.

## Prerequisites

1. **SAP BTP Trial Account**
   - Sign up at: https://account.hanatrial.ondemand.com/
   - Enable Cloud Foundry environment in your subaccount

2. **Cloud Foundry CLI**
   - Download from: https://docs.cloudfoundry.org/cf-cli/install-go-cli.html
   - Verify installation: `cf --version`

3. **Java 21 and Maven**
   - Java 21 JDK installed
   - Maven 3.8+ installed

## Service Bindings

The application requires the following SAP BTP services:

| Service | Plan | Instance Name | Description |
|---------|------|---------------|-------------|
| HANA DB | hana-free | lms-hana-db | SAP HANA Cloud database |
| Application Logging | lite | lms-application-logging | Centralized logging service |
| Application Autoscaler | standard | lms-application-autoscaler | Auto-scaling based on metrics |
| Destination | lite | lms-destination | External connectivity management |
| Feature Flags | lite | lms-feature-flags | Feature toggle management |
| User-Provided | - | lms-smtp-credentials | SMTP server credentials |

## Deployment Steps

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

# 2. Application Logging Service
cf create-service application-logs lite lms-application-logging

# 3. Application Autoscaler
cf create-service autoscaler standard lms-application-autoscaler

# 4. Destination Service
cf create-service destination lite lms-destination

# 5. Feature Flags Service
cf create-service feature-flags lite lms-feature-flags

# 6. User-Provided SMTP Credentials Service
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

### Step 4: Deploy to Cloud Foundry

```bash
# Deploy using manifest.yaml
cf push

# Or deploy with specific options
cf push -f manifest.yaml
```

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

### Common Issues

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

## Additional Resources

- [SAP BTP Documentation](https://help.sap.com/btp)
- [Cloud Foundry Documentation](https://docs.cloudfoundry.org/)
- [SAP HANA Cloud](https://help.sap.com/docs/hana-cloud)
- [Application Autoscaler](https://help.sap.com/docs/Application_Autoscaler)
- [Feature Flags Service](https://help.sap.com/docs/feature-flags-service)
- [Destination Service](https://help.sap.com/docs/connectivity/sap-btp-connectivity-cf/consuming-destination-service)
