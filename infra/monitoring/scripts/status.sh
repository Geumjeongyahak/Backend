#!/usr/bin/env bash

services=(prometheus loki tempo alloy grafana)
if systemctl --user list-unit-files alertmanager.service --no-legend | grep -q '^alertmanager\.service'; then
  services=(prometheus alertmanager loki tempo alloy grafana)
fi

systemctl --user status "${services[@]}" --no-pager
echo
podman ps
