#!/bin/bash

# SAP BTP Cloud Foundry Service Creation Script
# This script creates all required services for the Learning Management System application
#
# Usage:
#   ./cf-services.sh              - Create all services
#   ./cf-services.sh --get-flag <flag-name>  - Get a specific feature flag value
#   ./cf-services.sh --help                  - Show this help message

# Configuration
HANA_SERVICE_NAME="lms-hana-db"
LOGGING_SERVICE_NAME="lms-application-logging"
AUTOSCALER_SERVICE_NAME="lms-application-autoscaler"
DESTINATION_SERVICE_NAME="lms-destination"
FEATURE_FLAGS_SERVICE_NAME="lms-feature-flags"
FEATURE_FLAGS_KEY_NAME="lms-feature-flags-key"
SMTP_USER_PROVIDED_SERVICE_NAME="lms-smtp-credentials"
XSUAA_SERVICE_NAME="lms-xsuaa"
SAAS_REGISTRY_SERVICE_NAME="lms-saas-registry"

# Function to extract JSON from cf service-key output
extract_service_key_json() {
    local service_name=$1
    local key_name=$2
    cf service-key "$service_name" "$key_name" 2>/dev/null | awk '/^{/,0'
}

# Function to parse credentials from service key JSON
parse_credential() {
    local json=$1
    local field=$2
    echo "$json" | jq -r ".credentials.$field // .$field // null" 2>/dev/null
}

# Function to get feature flag value
get_feature_flag() {
    local flag_name=$1
    
    echo "Getting feature flag: $flag_name"
    echo ""
    
    # Check if service key exists, create if not
    if ! cf service-key "$FEATURE_FLAGS_SERVICE_NAME" "$FEATURE_FLAGS_KEY_NAME" &> /dev/null; then
        echo "Creating service key..."
        cf create-service-key "$FEATURE_FLAGS_SERVICE_NAME" "$FEATURE_FLAGS_KEY_NAME" 2>/dev/null || {
            echo "Error: Could not create service key. Make sure '$FEATURE_FLAGS_SERVICE_NAME' service exists."
            exit 1
        }
    fi
    
    # Get credentials
    local credentials=$(extract_service_key_json "$FEATURE_FLAGS_SERVICE_NAME" "$FEATURE_FLAGS_KEY_NAME")
    local username=$(parse_credential "$credentials" "username")
    local password=$(parse_credential "$credentials" "password")
    local uri=$(parse_credential "$credentials" "uri")
    
    if [ -z "$username" ] || [ "$username" = "null" ]; then
        echo "Error: Could not parse Feature Flags service credentials"
        exit 1
    fi
    
    # Evaluate the flag - try v2 API first
    local response=$(curl -s -u "$username:$password" "$uri/api/v2/evaluate/$flag_name")
    
    # If v2 fails, try v1
    if echo "$response" | grep -q "404"; then
        response=$(curl -s -u "$username:$password" "$uri/api/v1/evaluate/$flag_name")
    fi
    
    # Check for errors
    if echo "$response" | jq -e '.error' &> /dev/null; then
        echo "Error: $(echo "$response" | jq -r '.error // .message // "Unknown error"')"
        echo ""
        echo "Flag '$flag_name' may not exist. Create it in the Feature Flags Dashboard."
        exit 1
    fi
    
    if echo "$response" | grep -q "404 page not found"; then
        echo "Error: Flag '$flag_name' not found."
        echo ""
        echo "The flag may not exist. Create it in the Feature Flags Dashboard:"
        echo "  1. Go to SAP BTP Cockpit → Services → Instances and Subscriptions"
        echo "  2. Find 'feature-flags-dashboard' subscription"
        echo "  3. Click 'Go to Application' to open the dashboard"
        echo "  4. Create a new flag with name: $flag_name"
        exit 1
    fi
    
    # Extract value
    local value=$(echo "$response" | jq -r '.value // .variation // "N/A"')
    
    echo "Flag: $flag_name"
    echo "Value: $value"
    
    # Show what this means for SMTP
    if [ "$flag_name" = "use-destination-smtp" ]; then
        echo ""
        if [ "$value" = "true" ]; then
            echo "→ SMTP source: Destination Service (lms-smtp)"
        else
            echo "→ SMTP source: User-Provided Service (lms-smtp-credentials)"
        fi
    fi
}

# Function to show help
show_help() {
    echo "SAP BTP Cloud Foundry Service Setup Script"
    echo ""
    echo "Usage:"
    echo "  ./cf-services.sh                         - Create all services"
    echo "  ./cf-services.sh --get-flag <flag-name>  - Get a specific feature flag value"
    echo "  ./cf-services.sh --help                  - Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./cf-services.sh --get-flag use-destination-smtp"
}

# Handle command line arguments
case "${1:-}" in
    --get-flag)
        if [ -z "$2" ]; then
            echo "Error: Flag name required"
            echo "Usage: ./cf-services.sh --get-flag <flag-name>"
            exit 1
        fi
        get_feature_flag "$2"
        exit 0
        ;;
    --help|-h)
        show_help
        exit 0
        ;;
    "")
        # Continue with service creation
        ;;
    *)
        echo "Unknown option: $1"
        show_help
        exit 1
        ;;
esac

# Main service creation logic
set -e

echo "=============================================="
echo "SAP BTP Cloud Foundry Service Setup"
echo "=============================================="

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
echo "[1/7] HANA DB Service"
# Note: For trial accounts, use 'hana' service with 'schema' plan
# For productive accounts, use 'hana-cloud' service with appropriate plan
create_service_if_not_exists "hana-cloud" "hana-free" "$HANA_SERVICE_NAME"

# 2. Application Logging Service
echo ""
echo "[2/7] Application Logging Service"
create_service_if_not_exists "application-logs" "lite" "$LOGGING_SERVICE_NAME"

# 3. Application Autoscaler
echo ""
echo "[3/7] Application Autoscaler Service"
create_service_if_not_exists "autoscaler" "standard" "$AUTOSCALER_SERVICE_NAME"

# 4. Destination Service
echo ""
echo "[4/7] Destination Service"
create_service_if_not_exists "destination" "lite" "$DESTINATION_SERVICE_NAME"

# 5. Feature Flags Service
echo ""
echo "[5/7] Feature Flags Service"
create_service_if_not_exists "feature-flags" "lite" "$FEATURE_FLAGS_SERVICE_NAME"

# 6. XSUAA Service (OAuth2 Authentication)
echo ""
echo "[6/7] XSUAA Service (OAuth2)"
# Create XSUAA service using xs-security.json configuration
if cf service "$XSUAA_SERVICE_NAME" &> /dev/null; then
    echo "✓ Service '$XSUAA_SERVICE_NAME' already exists"
else
    echo "Creating service '$XSUAA_SERVICE_NAME' (xsuaa - application)..."
    if [ -f "xs-security.json" ]; then
        cf create-service xsuaa application "$XSUAA_SERVICE_NAME" -c xs-security.json
        echo "✓ Service '$XSUAA_SERVICE_NAME' created"
    else
        echo "⚠ xs-security.json not found. Creating XSUAA with default config..."
        cf create-service xsuaa application "$XSUAA_SERVICE_NAME"
        echo "✓ Service '$XSUAA_SERVICE_NAME' created (default config)"
    fi
fi

# 7. SaaS Provisioning Service (SaaS Registry)
echo ""
echo "[7/8] SaaS Registry Service (Multitenancy)"
# Create SaaS Registry service for marketplace registration
if cf service "$SAAS_REGISTRY_SERVICE_NAME" &> /dev/null; then
    echo "✓ Service '$SAAS_REGISTRY_SERVICE_NAME' already exists"
else
    echo "Creating service '$SAAS_REGISTRY_SERVICE_NAME' (saas-registry - application)..."
    if [ -f "saas-provisioning.json" ]; then
        cf create-service saas-registry application "$SAAS_REGISTRY_SERVICE_NAME" -c saas-provisioning.json
        echo "✓ Service '$SAAS_REGISTRY_SERVICE_NAME' created"
    else
        echo "⚠ saas-provisioning.json not found. Skipping SaaS Registry service."
        echo "  Create saas-provisioning.json to enable multitenancy features."
    fi
fi

# 8. User-Provided Service for SMTP Credentials
echo ""
echo "[8/8] User-Provided SMTP Credentials Service"
# SMTP credentials for Mailtrap
SMTP_CREDENTIALS='{
  "host": "sandbox.smtp.mailtrap.io",
  "port": "2525",
  "username": "0d4c2be927fce4",
  "password": "ff941ef75b43cf",
  "from": "no-reply@lms.example.com"
}'

if cf service "$SMTP_USER_PROVIDED_SERVICE_NAME" &> /dev/null; then
    echo "✓ User-provided service '$SMTP_USER_PROVIDED_SERVICE_NAME' already exists"
    echo "  Updating credentials..."
    cf update-user-provided-service "$SMTP_USER_PROVIDED_SERVICE_NAME" -p "$SMTP_CREDENTIALS"
    echo "✓ User-provided service '$SMTP_USER_PROVIDED_SERVICE_NAME' updated"
else
    echo "Creating user-provided service '$SMTP_USER_PROVIDED_SERVICE_NAME'..."
    cf create-user-provided-service "$SMTP_USER_PROVIDED_SERVICE_NAME" -p "$SMTP_CREDENTIALS"
    echo "✓ User-provided service '$SMTP_USER_PROVIDED_SERVICE_NAME' created"
fi

echo ""
echo "=============================================="
echo "All services created successfully!"
echo "=============================================="
echo ""
echo "Services summary:"
cf services
echo ""
echo "Next steps:"
echo "1. Build the application: mvn clean package"
echo "2. Deploy to CF: cf push"
echo "3. Check application status: cf apps"
echo "4. View logs: cf logs learning-management-system --recent"
echo "5. Check feature flag: ./cf-services.sh --get-flag use-destination-smtp"
echo ""
echo "IMPORTANT: To access the API, you need to:"
echo "  1. Assign the 'LMS_User' role collection to your user in SAP BTP Cockpit"
echo "  2. Go to: Security → Trust Configuration → Your IDP → Assign Role Collection"
echo "  3. Get an OAuth2 token from XSUAA to make authenticated API requests"
echo ""
echo "For ADMIN access (application-info endpoint):"
echo "  - Assign the 'LMS_Admin' role collection to your user"
echo ""
echo "For Multitenancy:"
echo "  - The SaaS Registry service enables subscription from other subaccounts"
echo "  - Create a subscriber subaccount and subscribe via BTP Cockpit"
