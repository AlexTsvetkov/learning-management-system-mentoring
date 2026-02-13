#!/bin/bash

# SAP BTP Cloud Foundry Service Creation Script
# This script creates all required services for the Learning Management System application

set -e

echo "=============================================="
echo "SAP BTP Cloud Foundry Service Setup"
echo "=============================================="

# Configuration
HANA_SERVICE_NAME="lms-hana-db"
LOGGING_SERVICE_NAME="lms-application-logging"
AUTOSCALER_SERVICE_NAME="lms-application-autoscaler"
DESTINATION_SERVICE_NAME="lms-destination"
FEATURE_FLAGS_SERVICE_NAME="lms-feature-flags"

# Check if CF CLI is installed
if ! command -v cf &> /dev/null; then
    echo "Error: CF CLI is not installed. Please install it first."
    echo "Visit: https://docs.cloudfoundry.org/cf-cli/install-go-cli.html"
    exit 1
fi

# Check if logged in
if ! cf target &> /dev/null; then
    echo "Error: Not logged in to Cloud Foundry. Please run 'cf login' first."
    exit 1
fi

echo ""
echo "Current CF target:"
cf target
echo ""

# Function to create service if it doesn't exist
create_service_if_not_exists() {
    local service_type=$1
    local plan=$2
    local service_name=$3
    local params=${4:-""}
    
    if cf service "$service_name" &> /dev/null; then
        echo "✓ Service '$service_name' already exists"
    else
        echo "Creating service '$service_name' ($service_type - $plan)..."
        if [ -n "$params" ]; then
            cf create-service "$service_type" "$plan" "$service_name" -c "$params"
        else
            cf create-service "$service_type" "$plan" "$service_name"
        fi
        echo "✓ Service '$service_name' created"
    fi
}

echo "Creating SAP BTP Services..."
echo ""

# 1. SAP HANA Cloud Database (schema plan for trial)
echo "[1/5] HANA DB Service"
# Note: For trial accounts, use 'hana' service with 'schema' plan
# For productive accounts, use 'hana-cloud' service with appropriate plan
#create_service_if_not_exists "hana" "schema" "$HANA_SERVICE_NAME"
create_service_if_not_exists "hana-cloud" "hana-free" "$HANA_SERVICE_NAME"

# 2. Application Logging Service
echo ""
echo "[2/5] Application Logging Service"
create_service_if_not_exists "application-logs" "lite" "$LOGGING_SERVICE_NAME"

# 3. Application Autoscaler
echo ""
echo "[3/5] Application Autoscaler Service"
# Create autoscaler with default configuration
AUTOSCALER_CONFIG='{
  "instance_min_count": 1,
  "instance_max_count": 3,
  "scaling_rules": [
    {
      "metric_type": "cpu",
      "breach_duration_secs": 60,
      "threshold": 80,
      "operator": ">=",
      "cool_down_secs": 120,
      "adjustment": "+1"
    },
    {
      "metric_type": "cpu",
      "breach_duration_secs": 60,
      "threshold": 20,
      "operator": "<",
      "cool_down_secs": 120,
      "adjustment": "-1"
    }
  ]
}'
create_service_if_not_exists "autoscaler" "standard" "$AUTOSCALER_SERVICE_NAME"

# 4. Destination Service
echo ""
echo "[4/5] Destination Service"
create_service_if_not_exists "destination" "lite" "$DESTINATION_SERVICE_NAME"

# 5. Feature Flags Service
echo ""
echo "[5/5] Feature Flags Service"
create_service_if_not_exists "feature-flags" "lite" "$FEATURE_FLAGS_SERVICE_NAME"

echo ""
echo "=============================================="
echo "All services created successfully!"
echo "=============================================="
echo ""
echo "Services summary:"
cf services
echo ""
echo "Next steps:"
echo "1. Build the application: mvn clean package -P cloud"
echo "2. Deploy to CF: cf push"
echo "3. Check application status: cf apps"
echo "4. View logs: cf logs learning-management-system --recent"