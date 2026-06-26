#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:?usage: scripts/gcp/01_infra/01_provision-gcp.sh scripts/gcp/00_env/prod.env}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "env file not found: ${ENV_FILE}" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "${ENV_FILE}"

ROOT_DIR="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
STARTUP_APP_TEMPLATE="${ROOT_DIR}/scripts/gcp/05_app/00_startup-app-manual.sh"
STARTUP_DB_TEMPLATE="${ROOT_DIR}/scripts/gcp/04_db/00_startup-db-manual.sh"
STARTUP_APP_RENDERED="$(mktemp)"
STARTUP_DB_RENDERED="$(mktemp)"
trap 'rm -f "${STARTUP_APP_RENDERED}" "${STARTUP_DB_RENDERED}"' EXIT

require() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "missing required variable: ${name}" >&2
    exit 1
  fi
}

exists_instance() {
  gcloud compute instances describe "$1" \
    --project="${PROJECT_ID}" \
    --zone="${ZONE}" >/dev/null 2>&1
}

exists_address() {
  gcloud compute addresses describe "$1" \
    --project="${PROJECT_ID}" \
    --region="${REGION}" >/dev/null 2>&1
}

exists_bucket() {
  gcloud storage buckets describe "gs://$1" \
    --project="${PROJECT_ID}" >/dev/null 2>&1
}

ensure_storage_folders() {
  local bucket="$1"
  local folders="${2:-}"
  local placeholder

  if [[ -z "${folders}" ]]; then
    return
  fi

  placeholder="$(mktemp)"
  for folder in ${folders//,/ }; do
    folder="${folder#/}"
    folder="${folder%/}"
    [[ -n "${folder}" ]] || continue
    gcloud storage cp "${placeholder}" "gs://${bucket}/${folder}/.keep" \
      --project="${PROJECT_ID}" >/dev/null
  done
  rm -f "${placeholder}"
}

ensure_firewall() {
  local name="$1"
  shift
  if gcloud compute firewall-rules describe "${name}" --project="${PROJECT_ID}" >/dev/null 2>&1; then
    echo "firewall exists: ${name}"
    return
  fi
  gcloud compute firewall-rules create "${name}" \
    --project="${PROJECT_ID}" \
    --network="${NETWORK}" \
    "$@"
}

require PROJECT_ID
require REGION
require ZONE
require NETWORK
require SUBNET
require APP_INSTANCE_NAME
require DB_INSTANCE_NAME
require APP_MACHINE_TYPE
require DB_MACHINE_TYPE
require BOOT_DISK_SIZE_GB
require APP_STATIC_IP_NAME
require DB_INTERNAL_IP_NAME
require APP_NETWORK_TAG
require DB_NETWORK_TAG
require APP_SERVICE_ACCOUNT_NAME
require STORAGE_BUCKET_NAME
require STORAGE_LOCATION
require DEPLOY_OS_USER

gcloud config set project "${PROJECT_ID}" >/dev/null

echo "[1/8] Enable required APIs"
gcloud services enable \
  compute.googleapis.com \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  logging.googleapis.com \
  monitoring.googleapis.com \
  storage.googleapis.com \
  cloudresourcemanager.googleapis.com \
  --project="${PROJECT_ID}"

APP_SERVICE_ACCOUNT_EMAIL="${APP_SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

echo "[2/8] Ensure app service account"
if ! gcloud iam service-accounts describe "${APP_SERVICE_ACCOUNT_EMAIL}" --project="${PROJECT_ID}" >/dev/null 2>&1; then
  gcloud iam service-accounts create "${APP_SERVICE_ACCOUNT_NAME}" \
    --project="${PROJECT_ID}" \
    --display-name="GJLearn ${ENVIRONMENT:-env} app service account"
fi

echo "[3/8] Ensure storage bucket"
if ! exists_bucket "${STORAGE_BUCKET_NAME}"; then
  gcloud storage buckets create "gs://${STORAGE_BUCKET_NAME}" \
    --project="${PROJECT_ID}" \
    --location="${STORAGE_LOCATION}" \
    --uniform-bucket-level-access
fi

ensure_storage_folders "${STORAGE_BUCKET_NAME}" "${STORAGE_FOLDERS:-}"

gcloud storage buckets add-iam-policy-binding "gs://${STORAGE_BUCKET_NAME}" \
  --member="serviceAccount:${APP_SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/storage.objectAdmin" \
  --project="${PROJECT_ID}" >/dev/null

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${APP_SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/iam.serviceAccountTokenCreator" >/dev/null

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${APP_SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/logging.logWriter" >/dev/null

if [[ "${STORAGE_PUBLIC_READ:-false}" == "true" ]]; then
  gcloud storage buckets add-iam-policy-binding "gs://${STORAGE_BUCKET_NAME}" \
    --member="allUsers" \
    --role="roles/storage.objectViewer" \
    --project="${PROJECT_ID}" >/dev/null
fi

echo "[4/8] Ensure static IP addresses"
if ! exists_address "${APP_STATIC_IP_NAME}"; then
  gcloud compute addresses create "${APP_STATIC_IP_NAME}" \
    --project="${PROJECT_ID}" \
    --region="${REGION}"
fi

if [[ "${SKIP_DB_INSTANCE:-false}" != "true" ]] && ! exists_address "${DB_INTERNAL_IP_NAME}"; then
  DB_ADDRESS_ARGS=(
    --project="${PROJECT_ID}"
    --region="${REGION}"
    --subnet="${SUBNET}"
  )
  if [[ -n "${DB_INTERNAL_IP:-}" ]]; then
    DB_ADDRESS_ARGS+=(--addresses="${DB_INTERNAL_IP}")
  fi
  gcloud compute addresses create "${DB_INTERNAL_IP_NAME}" "${DB_ADDRESS_ARGS[@]}"
fi

APP_STATIC_IP="$(gcloud compute addresses describe "${APP_STATIC_IP_NAME}" --project="${PROJECT_ID}" --region="${REGION}" --format='value(address)')"
DB_INTERNAL_IP_ACTUAL=""
if [[ "${SKIP_DB_INSTANCE:-false}" != "true" ]]; then
  DB_INTERNAL_IP_ACTUAL="$(gcloud compute addresses describe "${DB_INTERNAL_IP_NAME}" --project="${PROJECT_ID}" --region="${REGION}" --format='value(address)')"
fi

echo "[5/8] Ensure firewall rules"
ensure_firewall "gjlearn-${ENVIRONMENT}-allow-http" \
  --allow="${HTTP_PORTS:-tcp:8080}" \
  --source-ranges="${HTTP_SOURCE_RANGES:-0.0.0.0/0}" \
  --target-tags="${APP_NETWORK_TAG}"

ensure_firewall "gjlearn-${ENVIRONMENT}-allow-tailscale-app" \
  --allow=udp:41641 \
  --source-ranges="0.0.0.0/0" \
  --target-tags="${APP_NETWORK_TAG}"

if [[ "${SKIP_DB_INSTANCE:-false}" != "true" ]]; then
  ensure_firewall "gjlearn-${ENVIRONMENT}-allow-tailscale-db" \
    --allow=udp:41641 \
    --source-ranges="0.0.0.0/0" \
    --target-tags="${DB_NETWORK_TAG}"

  ensure_firewall "gjlearn-${ENVIRONMENT}-allow-app-to-postgres" \
    --allow=tcp:5432 \
    --source-tags="${APP_NETWORK_TAG}" \
    --target-tags="${DB_NETWORK_TAG}"
fi

ensure_firewall "gjlearn-${ENVIRONMENT}-allow-ssh-app" \
  --allow=tcp:22 \
  --source-ranges="${SSH_SOURCE_RANGES:-0.0.0.0/0}" \
  --target-tags="${APP_NETWORK_TAG}"

if [[ "${SKIP_DB_INSTANCE:-false}" != "true" ]]; then
  ensure_firewall "gjlearn-${ENVIRONMENT}-allow-ssh-db" \
    --allow=tcp:22 \
    --source-ranges="${SSH_SOURCE_RANGES:-0.0.0.0/0}" \
    --target-tags="${DB_NETWORK_TAG}"
fi

ensure_firewall "gjlearn-${ENVIRONMENT}-allow-iap-ssh" \
  --allow=tcp:22 \
  --source-ranges=35.235.240.0/20 \
  --target-tags="${APP_NETWORK_TAG}"

if [[ "${SKIP_DB_INSTANCE:-false}" != "true" ]]; then
  ensure_firewall "gjlearn-${ENVIRONMENT}-allow-iap-ssh-db" \
    --allow=tcp:22 \
    --source-ranges=35.235.240.0/20 \
    --target-tags="${DB_NETWORK_TAG}"
fi

if [[ ! -f "${STARTUP_APP_TEMPLATE}" ]]; then
  echo "app startup template not found: ${STARTUP_APP_TEMPLATE}" >&2
  exit 1
fi
if [[ "${SKIP_DB_INSTANCE:-false}" != "true" && ! -f "${STARTUP_DB_TEMPLATE}" ]]; then
  echo "db startup template not found: ${STARTUP_DB_TEMPLATE}" >&2
  exit 1
fi

echo "[6/8] Render manual-runtime startup scripts"
{
  printf 'export DEPLOY_OS_USER=%q\n' "${DEPLOY_OS_USER}"
  printf 'export SERVER_APP_DIR=%q\n' "${SERVER_APP_DIR:-/home/${DEPLOY_OS_USER}/app-dev}"
  cat "${STARTUP_APP_TEMPLATE}"
} > "${STARTUP_APP_RENDERED}"

if [[ "${SKIP_DB_INSTANCE:-false}" != "true" ]]; then
  {
    printf 'export DEPLOY_OS_USER=%q\n' "${DEPLOY_OS_USER}"
    printf 'export SERVER_DB_DIR=%q\n' "${SERVER_DB_DIR:-/home/${DEPLOY_OS_USER}/db-dev}"
    cat "${STARTUP_DB_TEMPLATE}"
  } > "${STARTUP_DB_RENDERED}"
fi

echo "[7/8] Ensure GCE instances"
if ! exists_instance "${APP_INSTANCE_NAME}"; then
  gcloud compute instances create "${APP_INSTANCE_NAME}" \
    --project="${PROJECT_ID}" \
    --zone="${ZONE}" \
    --machine-type="${APP_MACHINE_TYPE}" \
    --network-interface="network=${NETWORK},subnet=${SUBNET},address=${APP_STATIC_IP}" \
    --boot-disk-size="${BOOT_DISK_SIZE_GB}GB" \
    --boot-disk-type="${BOOT_DISK_TYPE:-pd-standard}" \
    --boot-disk-device-name="${APP_BOOT_DISK_NAME:-${APP_INSTANCE_NAME}-boot}" \
    --image-family="${IMAGE_FAMILY:-ubuntu-2204-lts}" \
    --image-project="${IMAGE_PROJECT:-ubuntu-os-cloud}" \
    --service-account="${APP_SERVICE_ACCOUNT_EMAIL}" \
    --scopes=https://www.googleapis.com/auth/cloud-platform \
    --tags="${APP_NETWORK_TAG}" \
    --metadata-from-file=startup-script="${STARTUP_APP_RENDERED}"
else
  echo "instance exists: ${APP_INSTANCE_NAME}"
fi

if [[ "${SKIP_DB_INSTANCE:-false}" == "true" ]]; then
  echo "skip DB instance because SKIP_DB_INSTANCE=true"
else
  DB_NETWORK_INTERFACE="network=${NETWORK},subnet=${SUBNET},private-network-ip=${DB_INTERNAL_IP_ACTUAL}"
  if [[ "${DB_EXTERNAL_IP_ENABLED:-false}" != "true" ]]; then
    DB_NETWORK_INTERFACE+=",no-address"
  fi

  if ! exists_instance "${DB_INSTANCE_NAME}"; then
    gcloud compute instances create "${DB_INSTANCE_NAME}" \
      --project="${PROJECT_ID}" \
      --zone="${ZONE}" \
      --machine-type="${DB_MACHINE_TYPE}" \
      --network-interface="${DB_NETWORK_INTERFACE}" \
      --boot-disk-size="${BOOT_DISK_SIZE_GB}GB" \
      --boot-disk-type="${BOOT_DISK_TYPE:-pd-standard}" \
      --boot-disk-device-name="${DB_BOOT_DISK_NAME:-${DB_INSTANCE_NAME}-boot}" \
      --image-family="${IMAGE_FAMILY:-ubuntu-2204-lts}" \
      --image-project="${IMAGE_PROJECT:-ubuntu-os-cloud}" \
      --service-account="${APP_SERVICE_ACCOUNT_EMAIL}" \
      --scopes=https://www.googleapis.com/auth/cloud-platform \
      --tags="${DB_NETWORK_TAG}" \
      --metadata-from-file=startup-script="${STARTUP_DB_RENDERED}"
  else
    echo "instance exists: ${DB_INSTANCE_NAME}"
  fi
fi

echo "[8/8] Outputs"
"${ROOT_DIR}/scripts/gcp/01_infra/02_print-outputs.sh" "${ENV_FILE}"
