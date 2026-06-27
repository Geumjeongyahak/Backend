#!/usr/bin/env bash
set -euo pipefail

echo "[Prometheus]"
curl -fsS http://localhost:9090/-/ready && echo

if systemctl --user is-active --quiet alertmanager; then
  echo "[Alertmanager]"
  curl -fsS http://localhost:9093/-/ready && echo
fi

echo "[Loki]"
curl -fsS http://localhost:3100/ready && echo

echo "[Tempo]"
curl -fsS http://localhost:3200/ready && echo

echo "[Alloy]"
systemctl --user is-active alloy

echo "[Grafana]"
curl -fsSI http://localhost:3000 | head -n 1
