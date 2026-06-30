#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-$HOME/app-dev}"
SERVICE_NAME="${SERVICE_NAME:-gjlearn-app}"
APP_USER="${APP_USER:-$(id -un)}"
APP_GROUP="${APP_GROUP:-$(id -gn)}"
JAR_PATH="${JAR_PATH:-${APP_DIR}/app.jar}"
ENV_PATH="${ENV_PATH:-${APP_DIR}/.env}"

read_env_value() {
  local key="$1"
  local line value

  if [[ ! -f "${ENV_PATH}" ]]; then
    return 0
  fi

  while IFS= read -r line; do
    [[ "${line}" == "${key}="* ]] || continue
    value="${line#*=}"
    value="${value%$'\r'}"
    value="${value%\"}"
    value="${value#\"}"
    value="${value%\'}"
    value="${value#\'}"
    printf '%s' "${value}"
    return 0
  done < "${ENV_PATH}"
}

configure_tailscale() {
  local auth_key="${TAILSCALE_AUTHKEY:-}"
  local tags="${TAILSCALE_TAGS:-}"
  local accept_dns="${TAILSCALE_ACCEPT_DNS:-}"
  local advertise_routes="${TAILSCALE_ADVERTISE_ROUTES:-}"

  if [[ -z "${auth_key}" ]]; then
    auth_key="$(read_env_value TAILSCALE_AUTHKEY)"
  fi
  if [[ -z "${tags}" ]]; then
    tags="$(read_env_value TAILSCALE_TAGS)"
  fi
  if [[ -z "${accept_dns}" ]]; then
    accept_dns="$(read_env_value TAILSCALE_ACCEPT_DNS)"
  fi
  if [[ -z "${advertise_routes}" ]]; then
    advertise_routes="$(read_env_value TAILSCALE_ADVERTISE_ROUTES)"
  fi
  accept_dns="${accept_dns:-false}"

  if [[ -n "${advertise_routes}" ]]; then
    printf 'net.ipv4.ip_forward = 1\n' | sudo tee /etc/sysctl.d/99-gjlearn-tailscale-router.conf >/dev/null
    sudo sysctl --system >/dev/null
  fi

  sudo systemctl enable --now tailscaled

  if sudo tailscale ip -4 >/dev/null 2>&1; then
    sudo tailscale set --accept-dns="${accept_dns}"
    if [[ -n "${advertise_routes}" ]]; then
      sudo tailscale set --advertise-routes="${advertise_routes}" 2>/dev/null \
        || sudo tailscale up --reset --advertise-routes="${advertise_routes}" --accept-dns="${accept_dns}"
    fi
    if [[ -n "${tags}" ]]; then
      local tag_up_args=(--reset --advertise-tags="${tags}" --accept-dns="${accept_dns}")
      if [[ -n "${advertise_routes}" ]]; then
        tag_up_args+=(--advertise-routes="${advertise_routes}")
      fi
      sudo tailscale set --advertise-tags="${tags}" 2>/dev/null \
        || sudo tailscale up "${tag_up_args[@]}"
    fi
    echo "tailscale already authenticated: $(sudo tailscale ip -4)"
    return 0
  fi

  if [[ -n "${auth_key}" ]]; then
    local up_args=(--auth-key="${auth_key}" --accept-dns="${accept_dns}")
    if [[ -n "${tags}" ]]; then
      up_args+=(--advertise-tags="${tags}")
    fi
    if [[ -n "${advertise_routes}" ]]; then
      up_args+=(--advertise-routes="${advertise_routes}")
    fi
    sudo tailscale up "${up_args[@]}"
    echo "tailscale authenticated: $(sudo tailscale ip -4)"
    return 0
  fi

  cat <<'EOF'
tailscale is installed but not authenticated.
Run this once on the VM and open the printed URL:
  sudo tailscale up

For non-interactive setup, set TAILSCALE_AUTHKEY in the environment or in the app .env file, then rerun this script.
To advertise ACL tags, also set TAILSCALE_TAGS as a comma-separated list such as tag:gjlearn-app,tag:prod.
To route private DB traffic through the App VM, set TAILSCALE_ADVERTISE_ROUTES to the GCP subnet CIDR, then approve the route in Tailscale.
Set TAILSCALE_ACCEPT_DNS=false to preserve GCE DNS.
EOF
}

configure_cloud_logging() {
  local enabled="${CLOUD_LOGGING_ENABLED:-}"
  local log_upload_path="${LOG_UPLOAD_PATH:-}"
  local log_id="${CLOUD_LOGGING_LOG_ID:-}"

  if [[ -z "${enabled}" ]]; then
    enabled="$(read_env_value CLOUD_LOGGING_ENABLED)"
  fi
  enabled="${enabled:-true}"
  if [[ "${enabled}" != "true" ]]; then
    echo "cloud logging disabled by CLOUD_LOGGING_ENABLED=${enabled}"
    return 0
  fi

  if [[ -z "${log_upload_path}" ]]; then
    log_upload_path="$(read_env_value LOG_UPLOAD_PATH)"
  fi
  log_upload_path="${log_upload_path:-./logs/app/application.*.log}"
  if [[ "${log_upload_path}" != /* ]]; then
    log_upload_path="${APP_DIR}/${log_upload_path#./}"
  fi

  if [[ -z "${log_id}" ]]; then
    log_id="$(read_env_value CLOUD_LOGGING_LOG_ID)"
  fi
  log_id="${log_id:-gjlearn-app}"

  if ! dpkg -s google-cloud-ops-agent >/dev/null 2>&1; then
    curl -fsSLo /tmp/add-google-cloud-ops-agent-repo.sh \
      https://dl.google.com/cloudagents/add-google-cloud-ops-agent-repo.sh
    sudo bash /tmp/add-google-cloud-ops-agent-repo.sh --also-install
  fi

  sudo mkdir -p /etc/google-cloud-ops-agent
  sudo tee /etc/google-cloud-ops-agent/config.yaml >/dev/null <<EOF
logging:
  receivers:
    ${log_id}:
      type: files
      include_paths:
        - ${log_upload_path}
      record_log_file_path: true
  service:
    pipelines:
      ${log_id}:
        receivers:
          - ${log_id}
EOF

  sudo systemctl enable --now google-cloud-ops-agent
  sudo systemctl restart google-cloud-ops-agent
}

configure_opentelemetry_javaagent() {
  local enabled="${OTEL_JAVAAGENT_ENABLED:-}"
  local agent_path="${OTEL_JAVAAGENT_PATH:-}"
  local agent_url="${OTEL_JAVAAGENT_URL:-}"

  if [[ -z "${enabled}" ]]; then
    enabled="$(read_env_value OTEL_JAVAAGENT_ENABLED)"
  fi
  enabled="${enabled:-false}"
  if [[ "${enabled}" != "true" ]]; then
    echo "OpenTelemetry Java agent disabled by OTEL_JAVAAGENT_ENABLED=${enabled}"
    return 0
  fi

  if [[ -z "${agent_path}" ]]; then
    agent_path="$(read_env_value OTEL_JAVAAGENT_PATH)"
  fi
  agent_path="${agent_path:-/opt/opentelemetry-javaagent.jar}"

  if [[ -z "${agent_url}" ]]; then
    agent_url="$(read_env_value OTEL_JAVAAGENT_URL)"
  fi
  agent_url="${agent_url:-https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar}"

  if [[ ! -s "${agent_path}" ]]; then
    curl -fsSLo /tmp/opentelemetry-javaagent.jar "${agent_url}"
    sudo install -o root -g root -m 0644 /tmp/opentelemetry-javaagent.jar "${agent_path}"
  fi
}

configure_caddy() {
  local enabled="${ENABLE_CADDY:-}"
  local api_domain="${API_DOMAIN:-}"
  local app_port="${APP_PORT:-}"

  if [[ -z "${enabled}" ]]; then
    enabled="$(read_env_value ENABLE_CADDY)"
  fi
  enabled="${enabled:-true}"

  if [[ -z "${api_domain}" ]]; then
    api_domain="$(read_env_value API_DOMAIN)"
  fi
  if [[ -z "${app_port}" ]]; then
    app_port="$(read_env_value APP_PORT)"
  fi
  app_port="${app_port:-8080}"

  if [[ "${enabled}" != "true" || -z "${api_domain}" ]]; then
    echo "caddy disabled or API_DOMAIN empty; skipping reverse proxy setup"
    return 0
  fi

  if ! command -v caddy >/dev/null 2>&1; then
    sudo apt-get install -y debian-keyring debian-archive-keyring apt-transport-https
    sudo rm -f /usr/share/keyrings/caddy-stable-archive-keyring.gpg
    curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' \
      | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
    curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' \
      | sudo tee /etc/apt/sources.list.d/caddy-stable.list >/dev/null
    sudo apt-get update
    sudo apt-get install -y caddy
  fi

  sudo install -d -o root -g root -m 755 /etc/caddy
  if [[ -f /etc/caddy/Caddyfile ]]; then
    sudo cp /etc/caddy/Caddyfile "/etc/caddy/Caddyfile.bak.$(date +%Y%m%d%H%M%S)"
  fi

  sudo tee /etc/caddy/Caddyfile >/dev/null <<EOF
${api_domain} {
    reverse_proxy 127.0.0.1:${app_port}
}
EOF

  sudo caddy fmt --overwrite /etc/caddy/Caddyfile
  sudo caddy validate --config /etc/caddy/Caddyfile
  sudo systemctl enable --now caddy
  sudo systemctl reload caddy
}

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "missing jar: ${JAR_PATH}" >&2
  exit 1
fi

if [[ ! -f "${ENV_PATH}" ]]; then
  echo "missing env file: ${ENV_PATH}" >&2
  exit 1
fi

sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg openjdk-21-jre-headless prometheus-node-exporter
if ! command -v tailscale >/dev/null 2>&1; then
  curl -fsSL https://tailscale.com/install.sh | sh
fi
configure_tailscale
sudo systemctl enable --now prometheus-node-exporter
configure_opentelemetry_javaagent

mkdir -p "${APP_DIR}/logs/app"
sudo chown -R "${APP_USER}:${APP_GROUP}" "${APP_DIR}"
chmod 600 "${ENV_PATH}"
configure_cloud_logging
configure_caddy

sudo tee "/etc/systemd/system/${SERVICE_NAME}.service" >/dev/null <<EOF
[Unit]
Description=GJLearn Spring Boot API
After=network-online.target tailscaled.service
Wants=network-online.target tailscaled.service

[Service]
Type=simple
User=${APP_USER}
Group=${APP_GROUP}
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_PATH}
ExecStart=/bin/sh -c 'agent=""; if [ "\${OTEL_JAVAAGENT_ENABLED:-false}" = "true" ]; then agent="-javaagent:\${OTEL_JAVAAGENT_PATH:-/opt/opentelemetry-javaagent.jar}"; fi; exec /usr/bin/java \$agent -Dserver.port="\${APP_PORT}" -jar ${JAR_PATH}'
SuccessExitStatus=143
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable "${SERVICE_NAME}.service"
sudo systemctl restart "${SERVICE_NAME}.service"
sudo systemctl --no-pager --full status "${SERVICE_NAME}.service"
