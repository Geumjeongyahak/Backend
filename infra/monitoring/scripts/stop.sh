#!/usr/bin/env bash
set -euo pipefail

services=(grafana alloy tempo loki prometheus)
if systemctl --user list-unit-files alertmanager.service --no-legend | grep -q '^alertmanager\.service'; then
  services=(grafana alloy tempo loki alertmanager prometheus)
fi

systemctl --user stop "${services[@]}"
