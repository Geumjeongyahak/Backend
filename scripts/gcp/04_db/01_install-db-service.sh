#!/usr/bin/env bash
set -euo pipefail

DB_DIR="${DB_DIR:-$HOME/db-dev}"
ENV_PATH="${ENV_PATH:-${DB_DIR}/.env}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgresql}"
NODE_EXPORTER_SERVICE="${NODE_EXPORTER_SERVICE:-prometheus-node-exporter}"
POSTGRES_EXPORTER_SERVICE="${POSTGRES_EXPORTER_SERVICE:-prometheus-postgres-exporter}"

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
: "${APP_DB_CIDR:?missing APP_DB_CIDR}"
DB_PORT="${DB_PORT:-5432}"
DB_LISTEN_ADDRESS="${DB_LISTEN_ADDRESS:-*}"
POSTGRES_EXPORTER_PORT="${POSTGRES_EXPORTER_PORT:-9187}"

sudo apt-get update
sudo apt-get install -y ca-certificates curl postgresql postgresql-contrib prometheus-node-exporter prometheus-postgres-exporter

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
ARGS=--web.listen-address=:${POSTGRES_EXPORTER_PORT}
EOF
sudo chmod 600 /etc/default/prometheus-postgres-exporter

sudo systemctl enable --now "${NODE_EXPORTER_SERVICE}"
sudo systemctl enable --now "${POSTGRES_EXPORTER_SERVICE}"
sudo systemctl restart "${POSTGRES_EXPORTER_SERVICE}"

sudo systemctl --no-pager --full status "${POSTGRES_SERVICE}"
sudo systemctl --no-pager --full status "${NODE_EXPORTER_SERVICE}"
sudo systemctl --no-pager --full status "${POSTGRES_EXPORTER_SERVICE}"
