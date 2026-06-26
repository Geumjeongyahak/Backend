#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:?usage: scripts/gcp/07_deploy/00_deploy-env.sh scripts/gcp/00_env/prod.env}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "env file not found: ${ENV_FILE}" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "${ENV_FILE}"

ROOT_DIR="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
ENVIRONMENT="${ENVIRONMENT:?missing ENVIRONMENT}"
ENV_DIR="${ROOT_DIR}/scripts/gcp/00_env"
APP_ENV_FILE="${APP_ENV_FILE:-${ENV_DIR}/${ENVIRONMENT}.app.env}"
DB_ENV_FILE="${DB_ENV_FILE:-${ENV_DIR}/${ENVIRONMENT}.db.env}"
RUN_INFRA="${RUN_INFRA:-true}"
RENDER_ENVS="${RENDER_ENVS:-true}"
INSTALL_DB="${INSTALL_DB:-true}"
INSTALL_APP="${INSTALL_APP:-true}"
BUILD_JAR="${BUILD_JAR:-true}"
CONFIGURE_ALERTS="${CONFIGURE_ALERTS:-false}"
USE_IAP_FOR_DB="${USE_IAP_FOR_DB:-true}"
USE_IAP_FOR_APP="${USE_IAP_FOR_APP:-false}"

require() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "missing required variable: ${name}" >&2
    exit 1
  fi
}

has_placeholder_value() {
  local value="$1"
  [[ -z "${value}" ]] && return 0
  [[ "${value}" == CHANGE_ME* ]] && return 0
  [[ "${value}" == *"<"* ]] && return 0
  [[ "${value}" == *"your-gcp-project-id"* ]] && return 0
  [[ "${value}" == *"example.com"* ]] && return 0
  [[ "${value}" == *"YOUR_"* ]] && return 0
  return 1
}

read_env_value_from_file() {
  local file="$1"
  local key="$2"
  awk -F= -v key="${key}" '$1 == key {sub(/^[^=]*=/, ""); print; exit}' "${file}"
}

validate_no_placeholder() {
  local file="$1"
  local key="$2"
  local value
  value="$(read_env_value_from_file "${file}" "${key}")"
  if has_placeholder_value "${value}"; then
    echo "${file}: set a real value for ${key}" >&2
    exit 1
  fi
}

validate_infra_env() {
  for key in PROJECT_ID REGION ZONE NETWORK SUBNET APP_INSTANCE_NAME DB_INSTANCE_NAME APP_STATIC_IP_NAME DB_INTERNAL_IP_NAME APP_NETWORK_TAG DB_NETWORK_TAG APP_SERVICE_ACCOUNT_NAME STORAGE_BUCKET_NAME DEPLOY_OS_USER FRONTEND_ORIGIN FRONTEND_REDIRECT_URI; do
    require "${key}"
    if has_placeholder_value "${!key}"; then
      echo "${ENV_FILE}: set a real value for ${key}" >&2
      exit 1
    fi
  done

  if [[ "${ENVIRONMENT}" == "prod" || "${ENABLE_CADDY:-true}" == "true" ]]; then
    require API_DOMAIN
    if has_placeholder_value "${API_DOMAIN}"; then
      echo "${ENV_FILE}: set a real value for API_DOMAIN" >&2
      exit 1
    fi
  fi
}

validate_runtime_envs() {
  if [[ "${INSTALL_DB}" == "true" && "${SKIP_DB_INSTANCE:-false}" != "true" ]]; then
    for key in POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD APP_DB_CIDR; do
      validate_no_placeholder "${DB_ENV_FILE}" "${key}"
    done
  fi

  if [[ "${INSTALL_APP}" == "true" ]]; then
    for key in POSTGRES_HOST POSTGRES_PORT POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD JWT_SECRET JWE_SECRET GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET GOOGLE_REDIRECT_URI FRONTEND_REDIRECT_URI CORS_ALLOWED_ORIGINS; do
      validate_no_placeholder "${APP_ENV_FILE}" "${key}"
    done

    local firebase_enabled
    firebase_enabled="$(read_env_value_from_file "${APP_ENV_FILE}" FIREBASE_ENABLED)"
    if [[ "${firebase_enabled}" == "true" ]]; then
      for key in FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_BASE64 FIREBASE_WEB_API_KEY FIREBASE_WEB_AUTH_DOMAIN FIREBASE_WEB_PROJECT_ID FIREBASE_WEB_STORAGE_BUCKET FIREBASE_WEB_MESSAGING_SENDER_ID FIREBASE_WEB_APP_ID; do
        validate_no_placeholder "${APP_ENV_FILE}" "${key}"
      done
    fi
  fi
}

gcloud_scp_args() {
  local target="$1"
  local use_iap="$2"
  local args=(--project "${PROJECT_ID}" --zone "${ZONE}")
  if [[ "${use_iap}" == "true" ]]; then
    args+=(--tunnel-through-iap)
  fi
  printf '%s\0' "${args[@]}"
}

gcloud_ssh_args() {
  gcloud_scp_args "$@"
}

run_scp() {
  local use_iap="$1"
  shift
  local args=()
  while IFS= read -r -d '' arg; do args+=("${arg}"); done < <(gcloud_scp_args unused "${use_iap}")
  gcloud compute scp "$@" "${args[@]}"
}

run_ssh() {
  local instance="$1"
  local use_iap="$2"
  shift 2
  local args=()
  while IFS= read -r -d '' arg; do args+=("${arg}"); done < <(gcloud_ssh_args unused "${use_iap}")
  gcloud compute ssh "${instance}" "${args[@]}" --command "$*"
}

validate_infra_env

if [[ "${RUN_INFRA}" == "true" ]]; then
  "${ROOT_DIR}/scripts/gcp/01_infra/01_provision-gcp.sh" "${ENV_FILE}"
fi

if [[ "${RENDER_ENVS}" == "true" ]]; then
  "${ROOT_DIR}/scripts/gcp/03_env_render/00_render-server-env.sh" "${ENV_FILE}" db > "${DB_ENV_FILE}"
  "${ROOT_DIR}/scripts/gcp/03_env_render/00_render-server-env.sh" "${ENV_FILE}" app > "${APP_ENV_FILE}"
  chmod 600 "${DB_ENV_FILE}" "${APP_ENV_FILE}"
  cat <<EOF
Rendered runtime envs:
- ${DB_ENV_FILE}
- ${APP_ENV_FILE}
Edit these files, replace CHANGE_ME/placeholder values, then rerun:
  RENDER_ENVS=false $0 ${ENV_FILE}
EOF
  exit 2
fi

validate_runtime_envs

if [[ "${INSTALL_DB}" == "true" && "${SKIP_DB_INSTANCE:-false}" != "true" ]]; then
  run_scp "${USE_IAP_FOR_DB}" \
    "${ROOT_DIR}/scripts/gcp/04_db/01_install-db-service.sh" \
    "${DB_ENV_FILE}" \
    "${DB_INSTANCE_NAME}:~/db-dev/"
  run_ssh "${DB_INSTANCE_NAME}" "${USE_IAP_FOR_DB}" \
    "cd ~/db-dev && mv $(basename "${DB_ENV_FILE}") .env && chmod +x 01_install-db-service.sh && ./01_install-db-service.sh"
  run_ssh "${DB_INSTANCE_NAME}" "${USE_IAP_FOR_DB}" \
    "sudo -u postgres psql -d '${POSTGRES_DB:-geumjeongyahak}' -tAc 'select current_database();' && curl -fsS http://127.0.0.1:9100/metrics >/dev/null && curl -fsS http://127.0.0.1:9187/metrics >/dev/null && echo db-ok"
fi

if [[ "${BUILD_JAR}" == "true" ]]; then
  (cd "${ROOT_DIR}" && ./gradlew bootJar -x test)
fi

if [[ "${INSTALL_APP}" == "true" ]]; then
  run_scp "${USE_IAP_FOR_APP}" \
    "${ROOT_DIR}"/build/libs/*.jar \
    "${ROOT_DIR}/scripts/gcp/05_app/01_install-app-service.sh" \
    "${APP_ENV_FILE}" \
    "${APP_INSTANCE_NAME}:~/app-dev/"
  run_ssh "${APP_INSTANCE_NAME}" "${USE_IAP_FOR_APP}" \
    "cd ~/app-dev && mv $(basename "${APP_ENV_FILE}") .env && mv *.jar app.jar && chmod +x 01_install-app-service.sh && ./01_install-app-service.sh"
  run_ssh "${APP_INSTANCE_NAME}" "${USE_IAP_FOR_APP}" \
    "curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null && sudo systemctl is-active --quiet gjlearn-app && echo app-ok"
fi

if [[ "${CONFIGURE_ALERTS}" == "true" ]]; then
  "${ROOT_DIR}/scripts/gcp/06_observability/00_configure-cloud-alerts.sh" "${ENV_FILE}"
fi

"${ROOT_DIR}/scripts/gcp/01_infra/02_print-outputs.sh" "${ENV_FILE}"
cat <<EOF
Deployment complete for ENVIRONMENT=${ENVIRONMENT}.

If ENABLE_CADDY=true and API_DOMAIN is set, verify DNS points to the App static IP and then test:
  curl -fsS https://${API_DOMAIN}/actuator/health

If Caddy is disabled, test the direct app port instead:
  curl -fsS http://<APP_STATIC_IP>:8080/actuator/health
EOF
