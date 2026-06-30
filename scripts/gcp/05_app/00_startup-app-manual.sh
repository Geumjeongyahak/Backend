#!/usr/bin/env bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

if [[ "${EUID}" -ne 0 ]]; then
  exec sudo -E bash "$0" "$@"
fi

DEPLOY_USER="${DEPLOY_OS_USER:-min}"
APP_DIR="${SERVER_APP_DIR:-/home/${DEPLOY_USER}/app-dev}"

apt-get update
apt-get install -y \
  ca-certificates \
  curl \
  gnupg \
  lsb-release \
  openjdk-21-jre-headless \
  postgresql-client \
  prometheus-node-exporter

if ! command -v tailscale >/dev/null 2>&1; then
  curl -fsSL https://tailscale.com/install.sh | sh
fi

systemctl enable --now prometheus-node-exporter

mkdir -p "${APP_DIR}/logs/app" /opt/gjlearn

if id "${DEPLOY_USER}" >/dev/null 2>&1; then
  chown -R "${DEPLOY_USER}:${DEPLOY_USER}" "${APP_DIR}" /opt/gjlearn || true
fi
