#!/bin/bash
# Script to automatically update Postman cloud environment with XSUAA credentials from deployed app
# Usage: ./scripts/update-postman-env.sh

set -e

POSTMAN_ENV_FILE="postman/cloud.postman_environment.json"
APP_NAME="learning-management-system"

echo "🔄 Fetching XSUAA credentials from $APP_NAME..."

# Check if cf CLI is available and logged in
if ! command -v cf &> /dev/null; then
    echo "❌ Error: CF CLI not found. Please install Cloud Foundry CLI."
    exit 1
fi

# Get VCAP_SERVICES JSON from the app
# The cf env output has format: "VCAP_SERVICES: {" on one line, then JSON content, then "VCAP_APPLICATION: {"
CF_ENV_OUTPUT=$(cf env "$APP_NAME" 2>/dev/null)

if [ -z "$CF_ENV_OUTPUT" ]; then
    echo "❌ Error: Could not fetch environment. Make sure you're logged in and the app exists."
    exit 1
fi

# Extract JSON starting from { after VCAP_SERVICES: until VCAP_APPLICATION:
VCAP_SERVICES=$(echo "$CF_ENV_OUTPUT" | sed -n '/^VCAP_SERVICES:/,/^VCAP_APPLICATION:/p' | sed '1s/VCAP_SERVICES: //' | sed '/^VCAP_APPLICATION:/d' | tr -d '\n' | sed 's/  */ /g')

if [ -z "$VCAP_SERVICES" ] || [ "$VCAP_SERVICES" = "{" ]; then
    echo "❌ Error: Could not extract VCAP_SERVICES JSON. Make sure you're logged in and the app exists."
    exit 1
fi

# Extract XSUAA credentials using jq
XSUAA_CLIENTID=$(echo "$VCAP_SERVICES" | jq -r '.xsuaa[0].credentials.clientid // empty')
XSUAA_CLIENTSECRET=$(echo "$VCAP_SERVICES" | jq -r '.xsuaa[0].credentials.clientsecret // empty')
XSUAA_URL=$(echo "$VCAP_SERVICES" | jq -r '.xsuaa[0].credentials.url // empty')

# Extract Destination service credentials
DEST_CLIENTID=$(echo "$VCAP_SERVICES" | jq -r '.destination[0].credentials.clientid // empty')
DEST_CLIENTSECRET=$(echo "$VCAP_SERVICES" | jq -r '.destination[0].credentials.clientsecret // empty')
DEST_TOKEN_URL=$(echo "$VCAP_SERVICES" | jq -r '.destination[0].credentials.url // empty')
DEST_URI=$(echo "$VCAP_SERVICES" | jq -r '.destination[0].credentials.uri // empty')

# Extract Feature Flags credentials
FF_USERNAME=$(echo "$VCAP_SERVICES" | jq -r '.["feature-flags"][0].credentials.username // empty')
FF_PASSWORD=$(echo "$VCAP_SERVICES" | jq -r '.["feature-flags"][0].credentials.password // empty')
FF_URI=$(echo "$VCAP_SERVICES" | jq -r '.["feature-flags"][0].credentials.uri // empty')

# Extract SaaS Registry credentials
SAAS_CLIENTID=$(echo "$VCAP_SERVICES" | jq -r '.["saas-registry"][0].credentials.clientid // empty')
SAAS_CLIENTSECRET=$(echo "$VCAP_SERVICES" | jq -r '.["saas-registry"][0].credentials.clientsecret // empty')
SAAS_URL=$(echo "$VCAP_SERVICES" | jq -r '.["saas-registry"][0].credentials.url // empty')

if [ -z "$XSUAA_CLIENTID" ] || [ -z "$XSUAA_CLIENTSECRET" ]; then
    echo "❌ Error: Could not extract XSUAA credentials from VCAP_SERVICES"
    exit 1
fi

echo "✅ Extracted XSUAA credentials:"
echo "   - Client ID: ${XSUAA_CLIENTID:0:30}..."
echo "   - URL: $XSUAA_URL"

# Check if Postman environment file exists
if [ ! -f "$POSTMAN_ENV_FILE" ]; then
    echo "❌ Error: Postman environment file not found: $POSTMAN_ENV_FILE"
    exit 1
fi

# Update the Postman environment file using jq
echo "📝 Updating $POSTMAN_ENV_FILE..."

# Create a temporary file with updates
jq --arg clientid "$XSUAA_CLIENTID" \
   --arg clientsecret "$XSUAA_CLIENTSECRET" \
   --arg url "$XSUAA_URL" \
   --arg dest_clientid "$DEST_CLIENTID" \
   --arg dest_clientsecret "$DEST_CLIENTSECRET" \
   --arg dest_token_url "$DEST_TOKEN_URL" \
   --arg dest_uri "$DEST_URI" \
   --arg ff_username "$FF_USERNAME" \
   --arg ff_password "$FF_PASSWORD" \
   --arg ff_uri "$FF_URI" \
   --arg saas_clientid "$SAAS_CLIENTID" \
   --arg saas_clientsecret "$SAAS_CLIENTSECRET" \
   --arg saas_url "$SAAS_URL" \
   '(.values[] | select(.key == "xsuaa_clientid") | .value) = $clientid |
    (.values[] | select(.key == "xsuaa_clientsecret") | .value) = $clientsecret |
    (.values[] | select(.key == "xsuaa_url") | .value) = $url |
    (.values[] | select(.key == "destination_clientid") | .value) = $dest_clientid |
    (.values[] | select(.key == "destination_clientsecret") | .value) = $dest_clientsecret |
    (.values[] | select(.key == "destination_token_url") | .value) = $dest_token_url |
    (.values[] | select(.key == "destination_uri") | .value) = $dest_uri |
    (.values[] | select(.key == "ff_username") | .value) = $ff_username |
    (.values[] | select(.key == "ff_password") | .value) = $ff_password |
    (.values[] | select(.key == "ff_uri") | .value) = $ff_uri |
    (.values[] | select(.key == "saas_registry_clientid") | .value) = $saas_clientid |
    (.values[] | select(.key == "saas_registry_clientsecret") | .value) = $saas_clientsecret |
    (.values[] | select(.key == "saas_registry_url") | .value) = $saas_url' \
   "$POSTMAN_ENV_FILE" > "${POSTMAN_ENV_FILE}.tmp" && mv "${POSTMAN_ENV_FILE}.tmp" "$POSTMAN_ENV_FILE"

echo "✅ Postman environment updated successfully!"
echo ""
echo "Updated credentials for:"
echo "  - XSUAA (xsuaa_clientid, xsuaa_clientsecret, xsuaa_url)"
echo "  - Destination Service (destination_clientid, destination_clientsecret, destination_token_url, destination_uri)"
echo "  - Feature Flags (ff_username, ff_password, ff_uri)"
echo "  - SaaS Registry (saas_registry_clientid, saas_registry_clientsecret, saas_registry_url)"
echo ""
echo "📦 Re-import $POSTMAN_ENV_FILE in Postman to use the new credentials."