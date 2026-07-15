#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
cd "${ROOT_DIR}"

assert_contains() {
  local pattern="$1"
  local file="$2"
  grep -Fq -- "${pattern}" "${file}" || {
    echo "expected ${file} to contain: ${pattern}" >&2
    exit 1
  }
}

assert_not_contains() {
  local pattern="$1"
  local file="$2"
  if grep -Fq -- "${pattern}" "${file}"; then
    echo "expected ${file} not to contain: ${pattern}" >&2
    exit 1
  fi
}

APP_INSTALL="scripts/gcp/05_app/01_install-app-service.sh"
DB_INSTALL="scripts/gcp/04_db/01_install-db-service.sh"
ENV_RENDER="scripts/gcp/03_env_render/00_render-server-env.sh"
MONITORING_SCRIPT="scripts/gcp/06_observability/00_configure-cloud-monitoring.sh"
PROVISION_SCRIPT="scripts/gcp/01_infra/01_provision-gcp.sh"
GRAFANA_DASHBOARD="scripts/gcp/05_app/grafana/dashboards/gjlearn.json"

assert_contains 'ENVIRONMENT=${ENVIRONMENT}' "${ENV_RENDER}"
assert_contains 'INTERNAL_GRAFANA_ENABLED=${INTERNAL_GRAFANA_ENABLED:-false}' "${ENV_RENDER}"
assert_not_contains 'OTEL_' "${ENV_RENDER}"

assert_contains 'type: prometheus' "${APP_INSTALL}"
assert_contains 'log_upload_path="${APP_DIR}/${log_upload_path#./}"' "${APP_INSTALL}"
assert_contains '127.0.0.1:9090' "${APP_INSTALL}"
assert_contains 'default_pipeline:' "${APP_INSTALL}"
assert_contains 'default_self_log_file_collection: false' "${APP_INSTALL}"
assert_contains 'agent.googleapis.com/processes/*' "${APP_INSTALL}"
assert_contains 'INTERNAL_GRAFANA_ENABLED' "${APP_INSTALL}"
assert_contains 'http_addr = 127.0.0.1' "${APP_INSTALL}"
assert_contains 'systemctl disable --now grafana-server' "${APP_INSTALL}"
assert_contains 'INTERNAL_GRAFANA_ADMIN_PASSWORD must be at least 16 characters' "${APP_INSTALL}"
assert_contains 'admin reset-admin-password' "${APP_INSTALL}"
assert_not_contains 'opentelemetry-javaagent' "${APP_INSTALL}"
assert_not_contains 'install -y ca-certificates curl gnupg openjdk-21-jre-headless prometheus-node-exporter' "${APP_INSTALL}"
assert_not_contains 'enable --now prometheus-node-exporter' "${APP_INSTALL}"

assert_contains 'google-cloud-ops-agent' "${DB_INSTALL}"
assert_contains 'default_pipeline:' "${DB_INSTALL}"
assert_contains 'default_self_log_file_collection: false' "${DB_INSTALL}"
assert_contains '127.0.0.1:${POSTGRES_EXPORTER_PORT}' "${DB_INSTALL}"
assert_not_contains 'postgresql-contrib prometheus-node-exporter' "${DB_INSTALL}"
assert_not_contains 'enable --now "${NODE_EXPORTER_SERVICE}"' "${DB_INSTALL}"

assert_contains 'address: ${MANAGEMENT_ADDRESS:127.0.0.1}' src/main/resources/application-prod.yml
assert_contains 'address: ${MANAGEMENT_ADDRESS:127.0.0.1}' src/main/resources/application-dev.yml

test -x "${MONITORING_SCRIPT}"
assert_contains 'monitoring dashboards' "${MONITORING_SCRIPT}"
assert_contains 'retention-days=30' "${MONITORING_SCRIPT}"
assert_contains 'jsonPayload.message=~' "${MONITORING_SCRIPT}"
assert_contains 'API 5xx ratio high' "${MONITORING_SCRIPT}"
assert_contains 'DB connections high' "${MONITORING_SCRIPT}"
assert_contains 'disableMetricValidation' "${MONITORING_SCRIPT}"
assert_contains 'resource.label.instance_id' "${MONITORING_SCRIPT}"
assert_contains 'SKIP_DB_INSTANCE' "${MONITORING_SCRIPT}"
assert_contains 'enable-private-ip-google-access' "${PROVISION_SCRIPT}"

jq empty "${GRAFANA_DASHBOARD}"
assert_contains '__ENVIRONMENT__' "${GRAFANA_DASHBOARD}"
test -f scripts/gcp/05_app/grafana/provisioning/datasources/gcp.yml
test ! -f infra/monitoring/prometheus/rules/gjlearn.yml
if find infra/monitoring/prometheus/targets/gjlearn -type f 2>/dev/null | grep -q .; then
  echo 'home Prometheus still has GJLearn target files' >&2
  exit 1
fi
grep -Fq 'job_name: app-actuator' infra/monitoring/prometheus/prometheus.yml && {
  echo 'home Prometheus still scrapes GJLearn' >&2
  exit 1
}

echo 'observability configuration contract passed'
