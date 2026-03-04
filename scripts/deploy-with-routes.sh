#!/bin/bash
# Deploy MTA and map subscriber routes
# Usage: ./scripts/deploy-with-routes.sh [subscriber-subdomain]
#
# This script:
# 1. Deploys the MTA archive
# 2. Maps subscriber routes to the approuter
#
# Default subscriber subdomain: lms-subscriber-pekroa1q

set -e

# Configuration
SUBSCRIBER_SUBDOMAIN="${1:-lms-subscriber-pekroa1q}"
APP_NAME="lms-approuter"
DOMAIN="cfapps.us10-001.hana.ondemand.com"
MTA_PATH="mta_archives/learning-management-system_0.1.0.mtar"

echo "======================================"
echo "LMS Deployment Script"
echo "======================================"
echo "Subscriber subdomain: ${SUBSCRIBER_SUBDOMAIN}"
echo "App name: ${APP_NAME}"
echo "Domain: ${DOMAIN}"
echo ""

# Step 1: Check if MTA exists
if [ ! -f "$MTA_PATH" ]; then
    echo "ERROR: MTA archive not found at $MTA_PATH"
    echo "Run 'mvn package -P cloud -DskipTests && mbt build' first"
    exit 1
fi

# Step 2: Deploy MTA
echo ">>> Step 1: Deploying MTA..."
cf deploy "$MTA_PATH"

# Step 3: Wait for apps to be running
echo ""
echo ">>> Step 2: Waiting for apps to be running..."
sleep 5

# Step 4: Map subscriber route
echo ""
echo ">>> Step 3: Mapping subscriber route..."
SUBSCRIBER_HOSTNAME="${SUBSCRIBER_SUBDOMAIN}"
SUBSCRIBER_ROUTE="${SUBSCRIBER_HOSTNAME}.${DOMAIN}"

# Check if route already mapped
EXISTING_ROUTES=$(cf routes | grep "$SUBSCRIBER_HOSTNAME" || true)
if [ -n "$EXISTING_ROUTES" ]; then
    echo "Route ${SUBSCRIBER_ROUTE} already exists"
else
    echo "Creating route ${SUBSCRIBER_ROUTE}..."
    cf create-route "${DOMAIN}" --hostname "${SUBSCRIBER_HOSTNAME}" 2>/dev/null || true
fi

# Map route to approuter
echo "Mapping route to ${APP_NAME}..."
cf map-route "${APP_NAME}" "${DOMAIN}" --hostname "${SUBSCRIBER_HOSTNAME}"

# Step 5: Show final routes
echo ""
echo "======================================"
echo "Deployment Complete!"
echo "======================================"
echo ""
echo "Routes configured for ${APP_NAME}:"
cf routes | head -1
cf routes | grep "${APP_NAME}"
echo ""
echo "Subscriber URL: https://${SUBSCRIBER_ROUTE}"
echo ""