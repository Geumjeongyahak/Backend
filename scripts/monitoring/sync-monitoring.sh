#!/usr/bin/env bash
set -euo pipefail

mode="${1:?usage: scripts/monitoring/sync-monitoring.sh diff|pull|push}"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
repo_monitoring="${repo_root}/infra/monitoring"
central_monitoring="${MONITORING_HOME:-/home/min/Infra/monitoring}"

exclude=(
  --exclude 'README.md'
  --exclude 'docker-compose.yml'
  --exclude 'grafana/data/'
  --exclude 'grafana/admin-login.txt'
  --exclude 'secrets/'
  --exclude 'caddy/*auth*'
)

case "$mode" in
  diff)
    rsync -ain --delete "${exclude[@]}" "${repo_monitoring}/" "${central_monitoring}/"
    ;;
  pull)
    rsync -a --delete "${exclude[@]}" "${central_monitoring}/" "${repo_monitoring}/"
    ;;
  push)
    rsync -a --delete "${exclude[@]}" "${repo_monitoring}/" "${central_monitoring}/"
    "${repo_root}/scripts/monitoring/apply-alert-env.sh" central
    ;;
  *)
    echo "usage: scripts/monitoring/sync-monitoring.sh diff|pull|push" >&2
    exit 2
    ;;
esac
