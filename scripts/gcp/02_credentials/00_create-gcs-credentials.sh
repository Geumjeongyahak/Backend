#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:?usage: scripts/gcp/02_credentials/00_create-gcs-credentials.sh scripts/gcp/00_env/dev.env [key-json-output-path]}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "env file not found: ${ENV_FILE}" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "${ENV_FILE}"

: "${PROJECT_ID:?missing PROJECT_ID}"
: "${ENVIRONMENT:?missing ENVIRONMENT}"
: "${APP_SERVICE_ACCOUNT_NAME:?missing APP_SERVICE_ACCOUNT_NAME}"
: "${STORAGE_BUCKET_NAME:?missing STORAGE_BUCKET_NAME}"
: "${STORAGE_LOCATION:?missing STORAGE_LOCATION}"

APP_SERVICE_ACCOUNT_EMAIL="${APP_SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
KEY_JSON_PATH="${2:-scripts/gcp/00_env/gjlearn-gcs-${ENVIRONMENT}-sa-key.json}"
ENV_SNIPPET_PATH="${ENV_SNIPPET_PATH:-scripts/gcp/00_env/gjlearn-gcs-${ENVIRONMENT}-credentials.env}"
APP_ENV_PATH="${APP_ENV_PATH:-}"
OVERWRITE_KEY="${OVERWRITE_KEY:-false}"
PRINT_JSON="${PRINT_JSON:-false}"
DRY_RUN="${DRY_RUN:-false}"

run() {
  if [[ "${DRY_RUN}" == "true" ]]; then
    printf '[dry-run]'
    printf ' %q' "$@"
    printf '\n'
  else
    "$@"
  fi
}

exists_service_account() {
  gcloud iam service-accounts describe "$1" --project="${PROJECT_ID}" >/dev/null 2>&1
}

exists_bucket() {
  gcloud storage buckets describe "gs://$1" --project="${PROJECT_ID}" >/dev/null 2>&1
}

base64_one_line() {
  if base64 --help 2>&1 | grep -q -- '-w'; then
    base64 -w0 "$1"
  else
    base64 "$1" | tr -d '\n'
  fi
}

ensure_storage_folders() {
  local bucket="$1"
  local folders="${2:-}"
  local placeholder

  if [[ -z "${folders}" ]]; then
    return 0
  fi

  placeholder="$(mktemp)"
  for folder in ${folders//,/ }; do
    folder="${folder#/}"
    folder="${folder%/}"
    [[ -n "${folder}" ]] || continue
    run gcloud storage cp "${placeholder}" "gs://${bucket}/${folder}/.keep" \
      --project="${PROJECT_ID}" >/dev/null
  done
  rm -f "${placeholder}"
}

update_app_env_if_requested() {
  local encoded="$1"
  local path="${APP_ENV_PATH}"

  if [[ -z "${path}" ]]; then
    return 0
  fi
  if [[ ! -f "${path}" ]]; then
    echo "APP_ENV_PATH does not exist: ${path}" >&2
    exit 1
  fi

  python - <<'PY' "${path}" "${encoded}" "${PROJECT_ID}" "${STORAGE_BUCKET_NAME}"
import sys
from pathlib import Path

path = Path(sys.argv[1])
encoded = sys.argv[2]
project_id = sys.argv[3]
bucket = sys.argv[4]
updates = {
    "GCP_PROJECT_ID": project_id,
    "GCP_PROD_BUCKET_NAME": bucket,
    "GCP_DEV_BUCKET_NAME": bucket,
    "GCP_ENCODED_CREDENTIALS": encoded,
}
lines = path.read_text(errors="ignore").splitlines()
seen = {key: False for key in updates}
out = []
for line in lines:
    if line and not line.lstrip().startswith("#") and "=" in line:
        key = line.split("=", 1)[0]
        if key in updates:
            out.append(f"{key}={updates[key]}")
            seen[key] = True
            continue
    out.append(line)
for key, value in updates.items():
    if not seen[key]:
        out.append(f"{key}={value}")
path.write_text("\n".join(out) + "\n")
path.chmod(0o600)
PY
  echo "updated app env with GCP credentials: ${path}"
}

gcloud config set project "${PROJECT_ID}" >/dev/null

echo "[1/5] Enable IAM, IAM Credentials, Storage APIs"
run gcloud services enable \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  storage.googleapis.com \
  cloudresourcemanager.googleapis.com \
  --project="${PROJECT_ID}"

echo "[2/5] Ensure service account: ${APP_SERVICE_ACCOUNT_EMAIL}"
if exists_service_account "${APP_SERVICE_ACCOUNT_EMAIL}"; then
  echo "service account exists: ${APP_SERVICE_ACCOUNT_EMAIL}"
else
  run gcloud iam service-accounts create "${APP_SERVICE_ACCOUNT_NAME}" \
    --project="${PROJECT_ID}" \
    --display-name="GJLearn ${ENVIRONMENT} app service account"
fi

echo "[3/5] Ensure GCS bucket and IAM"
if exists_bucket "${STORAGE_BUCKET_NAME}"; then
  echo "bucket exists: gs://${STORAGE_BUCKET_NAME}"
else
  run gcloud storage buckets create "gs://${STORAGE_BUCKET_NAME}" \
    --project="${PROJECT_ID}" \
    --location="${STORAGE_LOCATION}" \
    --uniform-bucket-level-access
fi

ensure_storage_folders "${STORAGE_BUCKET_NAME}" "${STORAGE_FOLDERS:-}"

run gcloud storage buckets add-iam-policy-binding "gs://${STORAGE_BUCKET_NAME}" \
  --member="serviceAccount:${APP_SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/storage.objectAdmin" \
  --project="${PROJECT_ID}" >/dev/null

run gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${APP_SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/iam.serviceAccountTokenCreator" >/dev/null

if [[ "${STORAGE_PUBLIC_READ:-false}" == "true" ]]; then
  run gcloud storage buckets add-iam-policy-binding "gs://${STORAGE_BUCKET_NAME}" \
    --member="allUsers" \
    --role="roles/storage.objectViewer" \
    --project="${PROJECT_ID}" >/dev/null
fi

echo "[4/5] Create service account key JSON"
if [[ -e "${KEY_JSON_PATH}" && "${OVERWRITE_KEY}" != "true" ]]; then
  echo "key json already exists: ${KEY_JSON_PATH}" >&2
  echo "Set OVERWRITE_KEY=true to replace it, or pass a different output path." >&2
  exit 1
fi
mkdir -p "$(dirname "${KEY_JSON_PATH}")"
if [[ "${OVERWRITE_KEY}" == "true" ]]; then
  rm -f "${KEY_JSON_PATH}"
fi
run gcloud iam service-accounts keys create "${KEY_JSON_PATH}" \
  --iam-account="${APP_SERVICE_ACCOUNT_EMAIL}" \
  --project="${PROJECT_ID}"
if [[ "${DRY_RUN}" != "true" ]]; then
  chmod 600 "${KEY_JSON_PATH}"
fi

echo "[5/5] Write base64 env snippet"
if [[ "${DRY_RUN}" != "true" ]]; then
  ENCODED_CREDENTIALS="$(base64_one_line "${KEY_JSON_PATH}")"
  umask 077
  cat > "${ENV_SNIPPET_PATH}" <<EOF
GCP_PROJECT_ID=${PROJECT_ID}
GCP_PROD_BUCKET_NAME=${STORAGE_BUCKET_NAME}
GCP_DEV_BUCKET_NAME=${STORAGE_BUCKET_NAME}
GCP_ENCODED_CREDENTIALS=${ENCODED_CREDENTIALS}
EOF
  chmod 600 "${ENV_SNIPPET_PATH}"
  update_app_env_if_requested "${ENCODED_CREDENTIALS}"
fi

cat <<EOF
GCS credential setup complete.
project=${PROJECT_ID}
bucket=gs://${STORAGE_BUCKET_NAME}
service_account=${APP_SERVICE_ACCOUNT_EMAIL}
key_json_path=${KEY_JSON_PATH}
env_snippet_path=${ENV_SNIPPET_PATH}

Security notes:
- The key JSON and env snippet contain secrets. Keep them out of git and delete/rotate if exposed.
- Prefer VM attached service-account credentials on GCE when possible; use this key only where encoded JSON is required.
EOF

if [[ "${PRINT_JSON}" == "true" && "${DRY_RUN}" != "true" ]]; then
  cat "${KEY_JSON_PATH}"
fi
