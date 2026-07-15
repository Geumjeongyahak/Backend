#!/usr/bin/env bash
set -euo pipefail

DB_DIR="${DB_DIR:-$HOME/db-dev}"
ENV_PATH="${ENV_PATH:-${DB_DIR}/.env}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgresql}"
POSTGRES_EXPORTER_SERVICE="${POSTGRES_EXPORTER_SERVICE:-prometheus-postgres-exporter}"

configure_tailscale() {
  local auth_key="${TAILSCALE_AUTHKEY:-}"
  local tags="${TAILSCALE_TAGS:-}"
  local accept_dns="${TAILSCALE_ACCEPT_DNS:-false}"

  sudo systemctl enable --now tailscaled

  if sudo tailscale ip -4 >/dev/null 2>&1; then
    sudo tailscale set --accept-dns="${accept_dns}"
    if [[ -n "${tags}" ]]; then
      sudo tailscale set --advertise-tags="${tags}" 2>/dev/null \
        || sudo tailscale up --reset --advertise-tags="${tags}" --accept-dns="${accept_dns}"
    fi
    echo "tailscale already authenticated: $(sudo tailscale ip -4)"
    return 0
  fi

  if [[ -n "${auth_key}" ]]; then
    local up_args=(--auth-key="${auth_key}" --accept-dns="${accept_dns}")
    if [[ -n "${tags}" ]]; then
      up_args+=(--advertise-tags="${tags}")
    fi
    sudo tailscale up "${up_args[@]}"
    echo "tailscale authenticated: $(sudo tailscale ip -4)"
    return 0
  fi

  cat <<'EOF'
tailscale is installed but not authenticated.
Run this once on the VM and open the printed URL:
  sudo tailscale up

For non-interactive setup, set TAILSCALE_AUTHKEY in the environment or in the db .env file, then rerun this script.
To advertise ACL tags, also set TAILSCALE_TAGS as a comma-separated list such as tag:gjlearn-db,tag:prod.
This simple-node setup intentionally does not advertise subnet routes; set TAILSCALE_ACCEPT_DNS=false to preserve GCE DNS.
EOF
}

configure_ops_agent() {
  if ! dpkg -s google-cloud-ops-agent >/dev/null 2>&1; then
    curl -fsSLo /tmp/add-google-cloud-ops-agent-repo.sh \
      https://dl.google.com/cloudagents/add-google-cloud-ops-agent-repo.sh
    sudo bash /tmp/add-google-cloud-ops-agent-repo.sh --also-install
  fi

  sudo mkdir -p /etc/google-cloud-ops-agent
  sudo tee /etc/google-cloud-ops-agent/config.yaml >/dev/null <<EOF
logging:
  service:
    pipelines:
      default_pipeline:
        receivers: []
metrics:
  receivers:
    hostmetrics:
      type: hostmetrics
      collection_interval: 60s
    gjlearn_postgres:
      type: prometheus
      config:
        scrape_configs:
          - job_name: gjlearn-postgres
            scrape_interval: 60s
            static_configs:
              - targets: ['127.0.0.1:${POSTGRES_EXPORTER_PORT}']
                labels:
                  project: gjlearn
                  env: ${ENVIRONMENT}
                  service: postgres
                  role: postgres-exporter
            metric_relabel_configs:
              - source_labels: [__name__]
                regex: 'up|pg_up|pg_stat_database_numbackends|pg_settings_max_connections|pg_stat_database_blks_(hit|read)|pg_stat_database_deadlocks|pg_database_size_bytes|pg_stat_database_xact_(commit|rollback)|pg_locks_count'
                action: keep
  processors:
    metrics_filter:
      type: exclude_metrics
      metrics_pattern:
        - agent.googleapis.com/interface/*
        - agent.googleapis.com/network/*
        - agent.googleapis.com/processes/*
        - agent.googleapis.com/swap/*
  service:
    pipelines:
      gjlearn_postgres:
        receivers:
          - gjlearn_postgres
global:
  default_self_log_file_collection: false
EOF
  sudo systemctl enable --now google-cloud-ops-agent
  sudo systemctl restart google-cloud-ops-agent
}

if [[ ! -f "${ENV_PATH}" ]]; then
  echo "missing env file: ${ENV_PATH}" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_PATH}"
set +a

: "${POSTGRES_DB:?missing POSTGRES_DB}"
: "${POSTGRES_USER:?missing POSTGRES_USER}"
: "${POSTGRES_PASSWORD:?missing POSTGRES_PASSWORD}"
: "${ENVIRONMENT:?missing ENVIRONMENT}"
DB_PORT="${DB_PORT:-5432}"
DB_LISTEN_ADDRESS="${DB_LISTEN_ADDRESS:-*}"
APP_DB_CIDR="${APP_DB_CIDR:-100.64.0.0/10}"
POSTGRES_EXPORTER_PORT="${POSTGRES_EXPORTER_PORT:-9187}"

sudo apt-get update
sudo apt-get install -y ca-certificates curl postgresql postgresql-contrib prometheus-postgres-exporter
if ! command -v tailscale >/dev/null 2>&1; then
  curl -fsSL https://tailscale.com/install.sh | sh
fi
configure_tailscale
if systemctl list-unit-files prometheus-node-exporter.service >/dev/null 2>&1; then
  sudo systemctl disable --now prometheus-node-exporter
  sudo apt-get remove -y prometheus-node-exporter
fi

PG_CONF_DIR="$(find /etc/postgresql -mindepth 2 -maxdepth 2 -type d -name main | sort -V | tail -n1)"
if [[ -z "${PG_CONF_DIR}" ]]; then
  echo "postgresql config directory not found" >&2
  exit 1
fi

sudo install -m 0644 -o root -g root "${PG_CONF_DIR}/postgresql.conf" "${PG_CONF_DIR}/postgresql.conf.before-gjlearn"
sudo install -m 0640 -o postgres -g postgres "${PG_CONF_DIR}/pg_hba.conf" "${PG_CONF_DIR}/pg_hba.conf.before-gjlearn"

sudo perl -0pi -e "s/^#?\s*listen_addresses\s*=.*/listen_addresses = '${DB_LISTEN_ADDRESS}'/m" "${PG_CONF_DIR}/postgresql.conf"
sudo perl -0pi -e "s/^#?\s*port\s*=.*/port = ${DB_PORT}/m" "${PG_CONF_DIR}/postgresql.conf"

sudo sed -i '/# gjlearn app access$/d' "${PG_CONF_DIR}/pg_hba.conf"
echo "host    ${POSTGRES_DB}    ${POSTGRES_USER}    ${APP_DB_CIDR}    scram-sha-256    # gjlearn app access" \
  | sudo tee -a "${PG_CONF_DIR}/pg_hba.conf" >/dev/null

sudo systemctl enable --now "${POSTGRES_SERVICE}"
sudo systemctl restart "${POSTGRES_SERVICE}"

sudo -u postgres psql -v ON_ERROR_STOP=1 -v db="${POSTGRES_DB}" -v user="${POSTGRES_USER}" -v pass="${POSTGRES_PASSWORD}" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'user', :'pass')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'user')\gexec
SELECT format('ALTER ROLE %I WITH PASSWORD %L', :'user', :'pass')\gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'db', :'user')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db')\gexec
SQL

sudo tee /etc/default/prometheus-postgres-exporter >/dev/null <<EOF
DATA_SOURCE_NAME=postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@localhost:${DB_PORT}/${POSTGRES_DB}?sslmode=disable
ARGS=--web.listen-address=127.0.0.1:${POSTGRES_EXPORTER_PORT}
EOF
sudo chmod 600 /etc/default/prometheus-postgres-exporter

sudo systemctl enable --now "${POSTGRES_EXPORTER_SERVICE}"
sudo systemctl restart "${POSTGRES_EXPORTER_SERVICE}"
configure_ops_agent

sudo systemctl --no-pager --full status "${POSTGRES_SERVICE}"
sudo systemctl --no-pager --full status "${POSTGRES_EXPORTER_SERVICE}"
