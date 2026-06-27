#!/usr/bin/env bash
set -euo pipefail

target="${1:-repo}"
env_file="${2:-infra/monitoring/monitoring.env}"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ ! -f "${repo_root}/${env_file}" && ! -f "$env_file" ]]; then
  echo "No monitoring env file found: ${env_file}"
  exit 0
fi

if [[ -f "${repo_root}/${env_file}" ]]; then
  env_file="${repo_root}/${env_file}"
fi

set -a
# shellcheck disable=SC1090
. "$env_file"
set +a

write_secret() {
  local root="$1" path="$2" value="$3"
  if [[ -z "$value" ]]; then
    rm -f "${root}/${path}"
    return 0
  fi
  mkdir -p "$(dirname "${root}/${path}")"
  printf '%s' "$value" > "${root}/${path}"
  chmod 600 "${root}/${path}"
  echo "wrote ${root}/${path}"
}

apply_to_root() {
  local root="$1"
  write_secret "$root" "secrets/alertmanager/gjlearn/default/discord/default" "${GJLEARN_DEFAULT_DISCORD_WEBHOOK:-}"

  write_secret "$root" "secrets/alertmanager/gjlearn/dev/discord/api" "${GJLEARN_DEV_API_DISCORD_WEBHOOK:-}"
  write_secret "$root" "secrets/alertmanager/gjlearn/dev/discord/app" "${GJLEARN_DEV_APP_DISCORD_WEBHOOK:-}"
  write_secret "$root" "secrets/alertmanager/gjlearn/dev/discord/db" "${GJLEARN_DEV_DB_DISCORD_WEBHOOK:-}"
  write_secret "$root" "secrets/alertmanager/gjlearn/dev/discord/postgres" "${GJLEARN_DEV_POSTGRES_DISCORD_WEBHOOK:-}"

  write_secret "$root" "secrets/alertmanager/gjlearn/prod/discord/api" "${GJLEARN_PROD_API_DISCORD_WEBHOOK:-}"
  write_secret "$root" "secrets/alertmanager/gjlearn/prod/discord/app" "${GJLEARN_PROD_APP_DISCORD_WEBHOOK:-}"
  write_secret "$root" "secrets/alertmanager/gjlearn/prod/discord/db" "${GJLEARN_PROD_DB_DISCORD_WEBHOOK:-}"
  write_secret "$root" "secrets/alertmanager/gjlearn/prod/discord/postgres" "${GJLEARN_PROD_POSTGRES_DISCORD_WEBHOOK:-}"
}

case "$target" in
  repo)
    apply_to_root "${repo_root}/infra/monitoring"
    ;;
  central)
    apply_to_root "${MONITORING_HOME:-/home/min/Infra/monitoring}"
    ;;
  both)
    apply_to_root "${repo_root}/infra/monitoring"
    apply_to_root "${MONITORING_HOME:-/home/min/Infra/monitoring}"
    ;;
  *)
    echo "usage: scripts/monitoring/apply-alert-env.sh repo|central|both [env-file]" >&2
    exit 2
    ;;
esac
