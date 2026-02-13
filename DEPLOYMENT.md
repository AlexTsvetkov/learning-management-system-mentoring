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
| HANA DB | schema | lms-hana-db | SAP HANA Cloud database (schema level) |
| Application Logging | lite | lms-application-logging | Centralized logging service |
| Application Autoscaler | standard | lms-application-autoscaler | Auto-scaling based on metrics |
| Destination | lite | lms-destination | External connectivity management |
| Feature Flags | lite | lms-feature-flags | Feature toggle management |

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
# 1. HANA DB (schema plan for trial)
cf create-service hana schema lms-hana-db

# 2. Application Logging Service
cf create-service application-logs lite lms-application-logging

# 3. Application Autoscaler
cf create-service autoscaler standard lms-application-autoscaler

# 4. Destination Service
cf create-service destination lite lms-destination

# 5. Feature Flags Service
cf create-service feature-flags lite lms-feature-flags
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

### Step 5: Configure Application Autoscaler (Optional)

```bash
# Attach autoscaler policy
cf attach-autoscaling-policy learning-management-system autoscaler-config.json

# Verify policy
cf autoscaling-policy learning-management-system
```

### Step 6: Verify Deployment

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

## Security Considerations

1. **VCAP_SERVICES**: Database credentials are automatically injected via Cloud Foundry service bindings
2. **No hardcoded secrets**: All sensitive configuration comes from environment variables
3. **HTTPS**: Cloud Foundry automatically provides HTTPS termination

## Additional Resources

- [SAP BTP Documentation](https://help.sap.com/btp)
- [Cloud Foundry Documentation](https://docs.cloudfoundry.org/)
- [SAP HANA Cloud](https://help.sap.com/docs/hana-cloud)
- [Application Autoscaler](https://help.sap.com/docs/Application_Autoscaler)