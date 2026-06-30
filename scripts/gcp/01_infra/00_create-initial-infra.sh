#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  scripts/gcp/01_infra/00_create-initial-infra.sh [env-file]

Creates the minimal initial GCP infrastructure for the jar/systemd + Tailscale deployment:
  - regional static external IP for the Spring/App VM
  - Spring/App GCE VM with a 10GB boot disk
  - PostgreSQL GCE VM with a 20GB boot disk and no external IP
  - prod GCS bucket
  - minimal firewall rules: app HTTP, Tailscale UDP 41641, IAP SSH, app-to-db PostgreSQL

Defaults match the requested prod names:
  APP_STATIC_IP_NAME=gjlearn-api-ip
  APP_INSTANCE_NAME=gjlearn-api-server-1
  DB_INSTANCE_NAME=gjlearn-postgres-db-1
  STORAGE_BUCKET_NAME=geumjeong-public-prod

Optional env-file variables override defaults. Example:
  PROJECT_ID=geumgeong-yahack scripts/gcp/01_infra/00_create-initial-infra.sh
  scripts/gcp/01_infra/00_create-initial-infra.sh scripts/gcp/00_env/prod.env

Set DRY_RUN=true to print commands without executing them.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

ENV_FILE="${1:-}"
if [[ -n "${ENV_FILE}" ]]; then
  if [[ ! -f "${ENV_FILE}" ]]; then
    echo "env file not found: ${ENV_FILE}" >&2
    exit 1
  fi
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
fi

PROJECT_ID="${PROJECT_ID:-$(gcloud config get-value project 2>/dev/null || true)}"
REGION="${REGION:-asia-northeast3}"
ZONE="${ZONE:-asia-northeast3-a}"
NETWORK="${NETWORK:-default}"
SUBNET="${SUBNET:-default}"

APP_STATIC_IP_NAME="${APP_STATIC_IP_NAME:-gjlearn-api-ip}"
APP_INSTANCE_NAME="${APP_INSTANCE_NAME:-gjlearn-api-server-1}"
APP_MACHINE_TYPE="${APP_MACHINE_TYPE:-e2-small}"
APP_BOOT_DISK_SIZE_GB="${APP_BOOT_DISK_SIZE_GB:-10}"
APP_NETWORK_TAG="${APP_NETWORK_TAG:-gjlearn-api-server}"
APP_BOOT_DISK_NAME="${APP_BOOT_DISK_NAME:-${APP_INSTANCE_NAME}-boot}"

DB_INSTANCE_NAME="${DB_INSTANCE_NAME:-gjlearn-postgres-db-1}"
DB_MACHINE_TYPE="${DB_MACHINE_TYPE:-e2-micro}"
DB_BOOT_DISK_SIZE_GB="${DB_BOOT_DISK_SIZE_GB:-20}"
DB_NETWORK_TAG="${DB_NETWORK_TAG:-gjlearn-postgres-db}"
DB_BOOT_DISK_NAME="${DB_BOOT_DISK_NAME:-${DB_INSTANCE_NAME}-boot}"
DB_INTERNAL_IP="${DB_INTERNAL_IP:-}"
DB_INTERNAL_IP_NAME="${DB_INTERNAL_IP_NAME:-}"

BOOT_DISK_TYPE="${BOOT_DISK_TYPE:-pd-standard}"
IMAGE_FAMILY="${IMAGE_FAMILY:-ubuntu-2404-lts-amd64}"
IMAGE_PROJECT="${IMAGE_PROJECT:-ubuntu-os-cloud}"

STORAGE_BUCKET_NAME="${STORAGE_BUCKET_NAME:-geumjeong-public-prod}"
STORAGE_LOCATION="${STORAGE_LOCATION:-asia-northeast3}"
STORAGE_PUBLIC_READ="${STORAGE_PUBLIC_READ:-false}"
STORAGE_FOLDERS="${STORAGE_FOLDERS:-profiles,editor,site-contents,documents/attachments,documents/purchase-items}"
APP_SERVICE_ACCOUNT_NAME="${APP_SERVICE_ACCOUNT_NAME:-gjlearn-api}"

HTTP_PORTS="${HTTP_PORTS:-tcp:8080}"
HTTP_SOURCE_RANGES="${HTTP_SOURCE_RANGES:-0.0.0.0/0}"
SSH_SOURCE_RANGES="${SSH_SOURCE_RANGES:-0.0.0.0/0}"
CREATE_PUBLIC_SSH_RULE="${CREATE_PUBLIC_SSH_RULE:-false}"
CREATE_FIREWALL_RULES="${CREATE_FIREWALL_RULES:-true}"
DRY_RUN="${DRY_RUN:-false}"

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

run() {
  if [[ "${DRY_RUN}" == "true" ]]; then
    printf '+ '
    printf '%q ' "$@"
    printf '\n'
  else
    "$@"
  fi
}

exists_address() {
  [[ "${DRY_RUN}" == "true" ]] && return 1
  gcloud compute addresses describe "$1" --project="${PROJECT_ID}" --region="${REGION}" >/dev/null 2>&1
}

exists_instance() {
  [[ "${DRY_RUN}" == "true" ]] && return 1
  gcloud compute instances describe "$1" --project="${PROJECT_ID}" --zone="${ZONE}" >/dev/null 2>&1
}

exists_bucket() {
  [[ "${DRY_RUN}" == "true" ]] && return 1
  gcloud storage buckets describe "gs://$1" --project="${PROJECT_ID}" >/dev/null 2>&1
}

exists_service_account() {
  [[ "${DRY_RUN}" == "true" ]] && return 1
  gcloud iam service-accounts describe "$1" --project="${PROJECT_ID}" >/dev/null 2>&1
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
    run gcloud storage cp "${placeholder}" "gs://${bucket}/${folder}/.keep" \
      --project="${PROJECT_ID}"
  done
  rm -f "${placeholder}"
}

ensure_firewall() {
  local name="$1"
  shift
  if [[ "${CREATE_FIREWALL_RULES}" != "true" ]]; then
    echo "skip firewall because CREATE_FIREWALL_RULES=false: ${name}"
    return
  fi
  if [[ "${DRY_RUN}" != "true" ]] && gcloud compute firewall-rules describe "${name}" --project="${PROJECT_ID}" >/dev/null 2>&1; then
    echo "firewall exists: ${name}"
    return
  fi
  run gcloud compute firewall-rules create "${name}" \
    --project="${PROJECT_ID}" \
    --network="${NETWORK}" \
    "$@"
}

require PROJECT_ID
require REGION
require ZONE
require NETWORK
require SUBNET

if [[ ! -f "${STARTUP_APP_TEMPLATE}" ]]; then
  echo "app startup template not found: ${STARTUP_APP_TEMPLATE}" >&2
  exit 1
fi
if [[ ! -f "${STARTUP_DB_TEMPLATE}" ]]; then
  echo "db startup template not found: ${STARTUP_DB_TEMPLATE}" >&2
  exit 1
fi

{
  printf 'export DEPLOY_OS_USER=%q\n' "${DEPLOY_OS_USER:-min}"
  printf 'export SERVER_APP_DIR=%q\n' "${SERVER_APP_DIR:-/home/${DEPLOY_OS_USER:-min}/app-dev}"
  cat "${STARTUP_APP_TEMPLATE}"
} > "${STARTUP_APP_RENDERED}"

{
  printf 'export DEPLOY_OS_USER=%q\n' "${DEPLOY_OS_USER:-min}"
  printf 'export SERVER_DB_DIR=%q\n' "${SERVER_DB_DIR:-/home/${DEPLOY_OS_USER:-min}/db-dev}"
  cat "${STARTUP_DB_TEMPLATE}"
} > "${STARTUP_DB_RENDERED}"

run gcloud config set project "${PROJECT_ID}"

run gcloud services enable \
  compute.googleapis.com \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  storage.googleapis.com \
  cloudresourcemanager.googleapis.com \
  --project="${PROJECT_ID}"

APP_SERVICE_ACCOUNT_EMAIL="${APP_SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

if exists_service_account "${APP_SERVICE_ACCOUNT_EMAIL}"; then
  echo "service account exists: ${APP_SERVICE_ACCOUNT_EMAIL}"
else
  run gcloud iam service-accounts create "${APP_SERVICE_ACCOUNT_NAME}" \
    --project="${PROJECT_ID}" \
    --display-name="GJLearn app service account"
fi

if exists_address "${APP_STATIC_IP_NAME}"; then
  echo "address exists: ${APP_STATIC_IP_NAME}"
else
  run gcloud compute addresses create "${APP_STATIC_IP_NAME}" \
    --project="${PROJECT_ID}" \
    --region="${REGION}"
fi

if exists_bucket "${STORAGE_BUCKET_NAME}"; then
  echo "bucket exists: gs://${STORAGE_BUCKET_NAME}"
else
  run gcloud storage buckets create "gs://${STORAGE_BUCKET_NAME}" \
    --project="${PROJECT_ID}" \
    --location="${STORAGE_LOCATION}" \
    --uniform-bucket-level-access
fi

ensure_storage_folders "${STORAGE_BUCKET_NAME}" "${STORAGE_FOLDERS}"

run gcloud storage buckets add-iam-policy-binding "gs://${STORAGE_BUCKET_NAME}" \
  --project="${PROJECT_ID}" \
  --member="serviceAccount:${APP_SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/storage.objectAdmin"

run gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${APP_SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/iam.serviceAccountTokenCreator"

if [[ "${STORAGE_PUBLIC_READ}" == "true" ]]; then
  run gcloud storage buckets add-iam-policy-binding "gs://${STORAGE_BUCKET_NAME}" \
    --project="${PROJECT_ID}" \
    --member="allUsers" \
    --role="roles/storage.objectViewer"
fi

APP_STATIC_IP="${APP_STATIC_IP:-}"
if [[ "${DRY_RUN}" == "true" ]]; then
  APP_STATIC_IP="${APP_STATIC_IP:-${APP_STATIC_IP_NAME}}"
else
  APP_STATIC_IP="$(gcloud compute addresses describe "${APP_STATIC_IP_NAME}" --project="${PROJECT_ID}" --region="${REGION}" --format='value(address)')"
fi

ensure_firewall "gjlearn-prod-allow-http" \
  --allow="${HTTP_PORTS}" \
  --source-ranges="${HTTP_SOURCE_RANGES}" \
  --target-tags="${APP_NETWORK_TAG}"

ensure_firewall "gjlearn-prod-allow-tailscale-app" \
  --allow=udp:41641 \
  --source-ranges="0.0.0.0/0" \
  --target-tags="${APP_NETWORK_TAG}"

ensure_firewall "gjlearn-prod-allow-app-to-postgres" \
  --allow=tcp:5432 \
  --source-tags="${APP_NETWORK_TAG}" \
  --target-tags="${DB_NETWORK_TAG}"

if [[ "${CREATE_PUBLIC_SSH_RULE}" == "true" ]]; then
  ensure_firewall "gjlearn-prod-allow-ssh-app" \
    --allow=tcp:22 \
    --source-ranges="${SSH_SOURCE_RANGES}" \
    --target-tags="${APP_NETWORK_TAG}"
fi

ensure_firewall "gjlearn-prod-allow-iap-ssh" \
  --allow=tcp:22 \
  --source-ranges=35.235.240.0/20 \
  --target-tags="${APP_NETWORK_TAG}"

ensure_firewall "gjlearn-prod-allow-iap-ssh-db" \
  --allow=tcp:22 \
  --source-ranges=35.235.240.0/20 \
  --target-tags="${DB_NETWORK_TAG}"

if exists_instance "${APP_INSTANCE_NAME}"; then
  echo "instance exists: ${APP_INSTANCE_NAME}"
else
  APP_FORWARDING_ARGS=()
  if [[ -n "${APP_TAILSCALE_ROUTES:-}" ]]; then
    APP_FORWARDING_ARGS+=(--can-ip-forward)
  fi

  run gcloud compute instances create "${APP_INSTANCE_NAME}" \
    --project="${PROJECT_ID}" \
    --zone="${ZONE}" \
    --machine-type="${APP_MACHINE_TYPE}" \
    --boot-disk-size="${APP_BOOT_DISK_SIZE_GB}GB" \
    --boot-disk-type="${BOOT_DISK_TYPE}" \
    --boot-disk-device-name="${APP_BOOT_DISK_NAME}" \
    --image-family="${IMAGE_FAMILY}" \
    --image-project="${IMAGE_PROJECT}" \
    --address="${APP_STATIC_IP}" \
    --network="${NETWORK}" \
    --subnet="${SUBNET}" \
    --service-account="${APP_SERVICE_ACCOUNT_EMAIL}" \
    --scopes=https://www.googleapis.com/auth/cloud-platform \
    --tags="${APP_NETWORK_TAG}" \
    "${APP_FORWARDING_ARGS[@]}" \
    --metadata-from-file=startup-script="${STARTUP_APP_RENDERED}"
fi

if exists_instance "${DB_INSTANCE_NAME}"; then
  echo "instance exists: ${DB_INSTANCE_NAME}"
else
  DB_NETWORK_ARGS=(--network="${NETWORK}" --subnet="${SUBNET}" --no-address)
  if [[ -n "${DB_INTERNAL_IP}" ]]; then
    DB_NETWORK_ARGS+=(--private-network-ip="${DB_INTERNAL_IP}")
  elif [[ -n "${DB_INTERNAL_IP_NAME}" && "${DRY_RUN}" != "true" ]]; then
    if gcloud compute addresses describe "${DB_INTERNAL_IP_NAME}" --project="${PROJECT_ID}" --region="${REGION}" >/dev/null 2>&1; then
      DB_NETWORK_ARGS+=(--private-network-ip="$(gcloud compute addresses describe "${DB_INTERNAL_IP_NAME}" --project="${PROJECT_ID}" --region="${REGION}" --format='value(address)')")
    fi
  fi

  run gcloud compute instances create "${DB_INSTANCE_NAME}" \
    --project="${PROJECT_ID}" \
    --zone="${ZONE}" \
    --machine-type="${DB_MACHINE_TYPE}" \
    --boot-disk-size="${DB_BOOT_DISK_SIZE_GB}GB" \
    --boot-disk-type="${BOOT_DISK_TYPE}" \
    --boot-disk-device-name="${DB_BOOT_DISK_NAME}" \
    --image-family="${IMAGE_FAMILY}" \
    --image-project="${IMAGE_PROJECT}" \
    "${DB_NETWORK_ARGS[@]}" \
    --tags="${DB_NETWORK_TAG}" \
    --metadata-from-file=startup-script="${STARTUP_DB_RENDERED}"
fi

if [[ "${DRY_RUN}" != "true" ]]; then
  echo
  echo "Created/verified resources:"
  gcloud compute addresses describe "${APP_STATIC_IP_NAME}" --project="${PROJECT_ID}" --region="${REGION}" --format='table(name,address,status)'
  gcloud compute instances list --project="${PROJECT_ID}" --filter="name:( ${APP_INSTANCE_NAME} ${DB_INSTANCE_NAME} )" --format='table(name,zone.basename(),machineType.basename(),status,networkInterfaces[0].networkIP,networkInterfaces[0].accessConfigs[0].natIP)'
  gcloud iam service-accounts describe "${APP_SERVICE_ACCOUNT_EMAIL}" --project="${PROJECT_ID}" --format='value(email)'
  gcloud storage buckets describe "gs://${STORAGE_BUCKET_NAME}" --project="${PROJECT_ID}" --format='value(name,location)'
fi

echo
echo "Next steps:"
echo "1. SSH into the App VM and run: sudo tailscale up --advertise-routes=<gcp-subnet-cidr>"
echo "2. Generate app/db env files with scripts/gcp/03_env_render/00_render-server-env.sh and copy them to ~/app-dev/.env and ~/db-dev/.env"
echo "3. Run scripts/gcp/04_db/01_install-db-service.sh on ${DB_INSTANCE_NAME}"
echo "4. Build and deploy the jar to ${APP_INSTANCE_NAME} via Tailscale/GitHub Actions"
