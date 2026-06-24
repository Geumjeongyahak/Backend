#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:?usage: scripts/gcp/render-server-env.sh scripts/gcp/env/prod.env app|db}"
TARGET="${2:?target must be app or db}"
# shellcheck disable=SC1090
source "${ENV_FILE}"

DB_INTERNAL_IP_ACTUAL="$(gcloud compute addresses describe "${DB_INTERNAL_IP_NAME}" --project="${PROJECT_ID}" --region="${REGION}" --format='value(address)' 2>/dev/null || true)"
APP_STATIC_IP="$(gcloud compute addresses describe "${APP_STATIC_IP_NAME}" --project="${PROJECT_ID}" --region="${REGION}" --format='value(address)' 2>/dev/null || true)"
API_BASE_URL="${API_DOMAIN:+https://${API_DOMAIN}}"
if [[ -z "${API_BASE_URL}" && -n "${APP_STATIC_IP}" ]]; then
  API_BASE_URL="http://${APP_STATIC_IP}:8080"
fi

case "${TARGET}" in
  app)
    cat <<EOF
SPRING_PROFILES_ACTIVE=${ENVIRONMENT}
APP_PORT=8080
MANAGEMENT_PORT=9090
NODE_EXPORTER_PORT=9100
APP_LOG_DIR=./logs/app
LOG_FILE_MAX_HISTORY=30
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=WARN
LOG_LEVEL_SPRING=WARN
LOG_LEVEL_SQL=WARN

POSTGRES_HOST=${DB_INTERNAL_IP_ACTUAL}
POSTGRES_PORT=5432
POSTGRES_DB=geumjeongyahak
POSTGRES_USER=postgres
POSTGRES_PASSWORD=CHANGE_ME
POSTGRES_OPTIONS=
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false

ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=CHANGE_ME_STRONG_12_CHARS_1A!
ADMIN_NAME=관리자

JWT_SECRET=CHANGE_ME_AT_LEAST_256_BITS
JWE_SECRET=CHANGE_ME_AT_LEAST_256_BITS
CORS_ALLOWED_ORIGINS=${FRONTEND_ORIGIN}

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=${API_BASE_URL}/api/v1/auth/google/callback
FRONTEND_REDIRECT_URI=${FRONTEND_REDIRECT_URI}

GCP_PROJECT_ID=${PROJECT_ID}
GCP_PROD_BUCKET_NAME=${STORAGE_BUCKET_NAME}
GCP_DEV_BUCKET_NAME=${STORAGE_BUCKET_NAME}
GCP_ENCODED_CREDENTIALS=

APP_IMAGE=ghcr.io/geumjeongyahak/backend:dev-latest
EOF
    ;;
  db)
    cat <<EOF
DB_PORT=5432
DB_BIND_ADDRESS=0.0.0.0
NODE_EXPORTER_PORT=9100
POSTGRES_DB=geumjeongyahak
POSTGRES_USER=postgres
POSTGRES_PASSWORD=CHANGE_ME
POSTGRES_VERSION=16-alpine
EOF
    ;;
  *)
    echo "unknown target: ${TARGET}" >&2
    exit 1
    ;;
esac
