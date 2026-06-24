#!/usr/bin/env bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

if ! command -v docker >/dev/null 2>&1; then
  apt-get update
  apt-get install -y ca-certificates curl gnupg
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  . /etc/os-release
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

systemctl enable --now docker

if id "${DEPLOY_OS_USER:-min}" >/dev/null 2>&1; then
  usermod -aG docker "${DEPLOY_OS_USER:-min}" || true
fi

mkdir -p "${SERVER_APP_DIR:-/home/min/app-dev}/logs/app"
mkdir -p "${SERVER_DB_DIR:-/home/min/db-dev}"

chown -R "${DEPLOY_OS_USER:-min}:${DEPLOY_OS_USER:-min}" \
  "${SERVER_APP_DIR:-/home/min/app-dev}" \
  "${SERVER_DB_DIR:-/home/min/db-dev}" || true
