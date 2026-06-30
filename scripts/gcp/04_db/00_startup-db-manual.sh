#!/usr/bin/env bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

if [[ "${EUID}" -ne 0 ]]; then
  exec sudo -E bash "$0" "$@"
fi

DEPLOY_USER="${DEPLOY_OS_USER:-min}"
DB_DIR="${SERVER_DB_DIR:-/home/${DEPLOY_USER}/db-dev}"

apt-get update
apt-get install -y \
  ca-certificates \
  curl \
  gnupg \
  lsb-release \
  postgresql \
  postgresql-contrib \
  prometheus-node-exporter \
  prometheus-postgres-exporter

systemctl enable --now prometheus-node-exporter

mkdir -p "${DB_DIR}" /opt/gjlearn

if id "${DEPLOY_USER}" >/dev/null 2>&1; then
  chown -R "${DEPLOY_USER}:${DEPLOY_USER}" "${DB_DIR}" /opt/gjlearn || true
fi
