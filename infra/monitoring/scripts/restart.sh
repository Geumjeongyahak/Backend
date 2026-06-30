#!/usr/bin/env bash
set -euo pipefail

systemctl --user daemon-reload

services=(prometheus loki tempo alloy grafana)
if systemctl --user list-unit-files alertmanager.service --no-legend | grep -q '^alertmanager\.service'; then
  services=(prometheus alertmanager loki tempo alloy grafana)
else
  echo "Alertmanager skipped: alertmanager.service is not loaded"
fi

systemctl --user reset-failed "${services[@]}" || true
systemctl --user restart "${services[@]}"

podman ps
