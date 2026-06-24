#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:?usage: scripts/gcp/print-outputs.sh scripts/gcp/env/prod.env}"
# shellcheck disable=SC1090
source "${ENV_FILE}"

APP_STATIC_IP="$(gcloud compute addresses describe "${APP_STATIC_IP_NAME}" --project="${PROJECT_ID}" --region="${REGION}" --format='value(address)' 2>/dev/null || true)"
DB_INTERNAL_IP_ACTUAL="$(gcloud compute addresses describe "${DB_INTERNAL_IP_NAME}" --project="${PROJECT_ID}" --region="${REGION}" --format='value(address)' 2>/dev/null || true)"
API_BASE_URL="${API_DOMAIN:+https://${API_DOMAIN}}"
if [[ -z "${API_BASE_URL}" && -n "${APP_STATIC_IP}" ]]; then
  API_BASE_URL="http://${APP_STATIC_IP}:8080"
fi

cat <<EOF
PROJECT_ID=${PROJECT_ID}
ENVIRONMENT=${ENVIRONMENT}
APP_INSTANCE_NAME=${APP_INSTANCE_NAME}
DB_INSTANCE_NAME=${DB_INSTANCE_NAME}
APP_EXTERNAL_IP=${APP_STATIC_IP}
DB_INTERNAL_IP=${DB_INTERNAL_IP_ACTUAL}
STORAGE_BUCKET_NAME=${STORAGE_BUCKET_NAME}
APP_SERVICE_ACCOUNT=${APP_SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com

Suggested app .env values:
POSTGRES_HOST=${DB_INTERNAL_IP_ACTUAL}
GCP_PROJECT_ID=${PROJECT_ID}
GCP_PROD_BUCKET_NAME=${STORAGE_BUCKET_NAME}
GCP_DEV_BUCKET_NAME=${STORAGE_BUCKET_NAME}
GOOGLE_REDIRECT_URI=${API_BASE_URL}/api/v1/auth/google/callback
FRONTEND_REDIRECT_URI=${FRONTEND_REDIRECT_URI}
CORS_ALLOWED_ORIGINS=${FRONTEND_ORIGIN}

OAuth Console:
- Authorized JavaScript origins: ${FRONTEND_ORIGIN}
- Authorized redirect URI: ${API_BASE_URL}/api/v1/auth/google/callback
EOF
