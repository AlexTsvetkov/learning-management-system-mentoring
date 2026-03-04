#!/bin/bash
# Post-deployment route mapping script
# Run this AFTER deployment is complete to map subscriber routes
# Usage: ./scripts/post-deploy-routes.sh [subscriber-subdomain]
#
# Default subscriber subdomain: lms-subscriber-pekroa1q

set -e

# Configuration
SUBSCRIBER_SUBDOMAIN="${1:-lms-subscriber-pekroa1q}"
APP_NAME="lms-approuter"
DOMAIN="cfapps.us10-001.hana.ondemand.com"

echo "======================================"
echo "Post-Deployment Route Mapping"
echo "======================================"
echo "Subscriber subdomain: ${SUBSCRIBER_SUBDOMAIN}"
echo "App name: ${APP_NAME}"
echo ""

# Map subscriber route
echo ">>> Mapping subscriber route..."
SUBSCRIBER_ROUTE="${SUBSCRIBER_SUBDOMAIN}.${DOMAIN}"

cf map-route "${APP_NAME}" "${DOMAIN}" --hostname "${SUBSCRIBER_SUBDOMAIN}"

# Show final routes
echo ""
echo "======================================"
echo "Routes Configured!"
echo "======================================"
echo ""
echo "Routes for ${APP_NAME}:"
cf routes | head -1
cf routes | grep "${APP_NAME}"
echo ""
echo "Provider URL:   https://0658761dtrial-dev-lms-approuter.${DOMAIN}"
echo "Subscriber URL: https://${SUBSCRIBER_ROUTE}"
echo ""