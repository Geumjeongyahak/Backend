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

  if [[ -z "${auth_key}" ]]; then
    auth_key="$(read_env_value TAILSCALE_AUTHKEY)"
  fi
  if [[ -z "${tags}" ]]; then
    tags="$(read_env_value TAILSCALE_TAGS)"
  fi
  if [[ -z "${accept_dns}" ]]; then
    accept_dns="$(read_env_value TAILSCALE_ACCEPT_DNS)"
  fi
  accept_dns="${accept_dns:-false}"

  sudo systemctl enable --now tailscaled

  if sudo tailscale ip -4 >/dev/null 2>&1; then
    sudo tailscale set --accept-dns="${accept_dns}"
    if [[ -n "${tags}" ]]; then
      sudo tailscale set --advertise-tags="${tags}" 2>/dev/null \
        || sudo tailscale up --reset --advertise-tags="${tags}" --accept-dns="${accept_dns}"
    fi
    echo "tailscale already authenticated: $(sudo tailscale ip -4)"
    return 0
  fi

  if [[ -n "${auth_key}" ]]; then
    local up_args=(--auth-key="${auth_key}" --accept-dns="${accept_dns}")
    if [[ -n "${tags}" ]]; then
      up_args+=(--advertise-tags="${tags}")
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
This simple-node setup intentionally does not advertise subnet routes; set TAILSCALE_ACCEPT_DNS=false to preserve GCE DNS.
EOF
}

configure_ops_agent() {
  local log_upload_path="${LOG_UPLOAD_PATH:-}"
  local log_id="${CLOUD_LOGGING_LOG_ID:-}"
  local environment="${ENVIRONMENT:-}"

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
  if [[ -z "${environment}" ]]; then
    environment="$(read_env_value ENVIRONMENT)"
  fi
  environment="${environment:-prod}"

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
      default_pipeline:
        receivers: []
      ${log_id}:
        receivers:
          - ${log_id}
metrics:
  receivers:
    hostmetrics:
      type: hostmetrics
      collection_interval: 60s
    gjlearn_app:
      type: prometheus
      config:
        scrape_configs:
          - job_name: gjlearn-app
            scrape_interval: 60s
            metrics_path: /actuator/prometheus
            static_configs:
              - targets: ['127.0.0.1:9090']
                labels:
                  project: gjlearn
                  env: ${environment}
                  service: api
                  role: app-actuator
            metric_relabel_configs:
              - source_labels: [__name__]
                regex: 'up|http_server_requests_seconds_(count|bucket)|hikaricp_connections|jvm_memory_used_bytes|jvm_threads_live_threads'
                action: keep
  processors:
    metrics_filter:
      type: exclude_metrics
      metrics_pattern:
        - agent.googleapis.com/interface/*
        - agent.googleapis.com/network/*
        - agent.googleapis.com/processes/*
        - agent.googleapis.com/swap/*
  service:
    pipelines:
      gjlearn_app:
        receivers:
          - gjlearn_app
global:
  default_self_log_file_collection: false
EOF

  sudo systemctl enable --now google-cloud-ops-agent
  sudo systemctl restart google-cloud-ops-agent
}

configure_internal_grafana() {
  local enabled password project_id environment
  enabled="$(read_env_value INTERNAL_GRAFANA_ENABLED)"
  enabled="${enabled:-false}"

  if [[ "${enabled}" != "true" ]]; then
    if systemctl list-unit-files grafana-server.service >/dev/null 2>&1; then
      sudo systemctl disable --now grafana-server
    fi
    echo "internal Grafana disabled"
    return 0
  fi

  password="$(read_env_value INTERNAL_GRAFANA_ADMIN_PASSWORD)"
  if [[ ${#password} -lt 16 || "${password}" == *$'\n'* ]]; then
    echo "INTERNAL_GRAFANA_ADMIN_PASSWORD must be at least 16 characters" >&2
    exit 1
  fi
  project_id="$(read_env_value GCP_PROJECT_ID)"
  : "${project_id:?missing GCP_PROJECT_ID}"
  environment="$(read_env_value ENVIRONMENT)"
  : "${environment:?missing ENVIRONMENT}"

  if ! dpkg -s grafana >/dev/null 2>&1; then
    sudo mkdir -p /etc/apt/keyrings
    sudo rm -f /etc/apt/keyrings/grafana.gpg
    curl -fsSL https://apt.grafana.com/gpg.key | sudo gpg --dearmor -o /etc/apt/keyrings/grafana.gpg
    echo "deb [signed-by=/etc/apt/keyrings/grafana.gpg] https://apt.grafana.com stable main" \
      | sudo tee /etc/apt/sources.list.d/grafana.list >/dev/null
    sudo apt-get update
    sudo apt-get install -y grafana
  fi

  test -f "${APP_DIR}/grafana/provisioning/datasources/gcp.yml"
  test -f "${APP_DIR}/grafana/provisioning/dashboards/dashboards.yml"
  test -f "${APP_DIR}/grafana/dashboards/gjlearn.json"
  sudo install -d -o root -g grafana -m 0750 \
    /etc/grafana/provisioning/datasources /etc/grafana/provisioning/dashboards /var/lib/grafana/dashboards
  sudo install -o root -g grafana -m 0640 "${APP_DIR}/grafana/provisioning/datasources/gcp.yml" \
    /etc/grafana/provisioning/datasources/gcp.yml
  sudo install -o root -g grafana -m 0640 "${APP_DIR}/grafana/provisioning/dashboards/dashboards.yml" \
    /etc/grafana/provisioning/dashboards/gjlearn.yml
  sed -e "s/__PROJECT_ID__/${project_id}/g" -e "s/__ENVIRONMENT__/${environment}/g" \
    "${APP_DIR}/grafana/dashboards/gjlearn.json" \
    | sudo tee /var/lib/grafana/dashboards/gjlearn.json >/dev/null
  sudo chown root:grafana /var/lib/grafana/dashboards/gjlearn.json
  sudo chmod 0640 /var/lib/grafana/dashboards/gjlearn.json

  sudo tee /etc/grafana/grafana.ini >/dev/null <<EOF
[server]
http_addr = 127.0.0.1
http_port = 3000
[security]
admin_user = admin
admin_password = ${password}
[users]
allow_sign_up = false
[auth.anonymous]
enabled = false
EOF
  sudo chown root:grafana /etc/grafana/grafana.ini
  sudo chmod 0640 /etc/grafana/grafana.ini
  sudo systemctl daemon-reload
  sudo systemctl enable --now grafana-server
  if command -v grafana >/dev/null 2>&1; then
    sudo -u grafana grafana cli --homepath /usr/share/grafana --config /etc/grafana/grafana.ini \
      --configOverrides cfg:default.paths.data=/var/lib/grafana \
      admin reset-admin-password "${password}"
  else
    sudo -u grafana grafana-cli --homepath /usr/share/grafana --config /etc/grafana/grafana.ini \
      --configOverrides cfg:default.paths.data=/var/lib/grafana \
      admin reset-admin-password "${password}"
  fi
  sudo systemctl restart grafana-server
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
sudo apt-get install -y ca-certificates curl gnupg openjdk-21-jre-headless
if ! command -v tailscale >/dev/null 2>&1; then
  curl -fsSL https://tailscale.com/install.sh | sh
fi
configure_tailscale
if systemctl list-unit-files prometheus-node-exporter.service >/dev/null 2>&1; then
  sudo systemctl disable --now prometheus-node-exporter
  sudo apt-get remove -y prometheus-node-exporter
fi

mkdir -p "${APP_DIR}/logs/app"
sudo chown -R "${APP_USER}:${APP_GROUP}" "${APP_DIR}"
chmod 600 "${ENV_PATH}"
configure_ops_agent
configure_internal_grafana
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
ExecStart=/usr/bin/java -Dserver.port=\${APP_PORT} -jar ${JAR_PATH}
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
