#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/gcp/03_env_render/01_configure-preinfra-env.sh scripts/gcp/00_env/dev.env

Interactive pre-server/provisioning configuration editor.
Use before creating or recreating GCE resources. It writes the selected ENV_FILE only.

Covers:
- GCP project/region/zone/network
- App/DB instance names, machine/disk/image settings
- static/internal IP and firewall source ranges
- frontend/API domains and Caddy toggle
- storage bucket/service account
- deploy OS paths/user
- Tailscale tags/basic node mode
- Cloud Logging alert names

Non-interactive helpers:
  GJLEARN_NONINTERACTIVE=true    keep existing/default answers
  GJLEARN_ENVIRONMENT=dev|prod   used when ENV_FILE does not exist
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

ENV_FILE="${1:?missing ENV_FILE. Try --help}"
ROOT_DIR="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
ENV_DIR="${ROOT_DIR}/scripts/gcp/00_env"
NONINTERACTIVE="${GJLEARN_NONINTERACTIVE:-false}"

mkdir -p "$(dirname "${ENV_FILE}")"

infer_environment() {
  local base
  base="$(basename "${ENV_FILE}")"
  case "${base}" in
    dev.env) printf 'dev' ;;
    prod.env) printf 'prod' ;;
    *) printf '%s' "${GJLEARN_ENVIRONMENT:-dev}" ;;
  esac
}

if [[ ! -f "${ENV_FILE}" ]]; then
  env_name="$(infer_environment)"
  example="${ENV_DIR}/${env_name}.env.example"
  if [[ ! -f "${example}" ]]; then
    echo "cannot create ${ENV_FILE}: example not found: ${example}" >&2
    exit 1
  fi
  cp "${example}" "${ENV_FILE}"
  chmod 600 "${ENV_FILE}"
  echo "created ${ENV_FILE} from ${example}"
fi

python3 - <<'PY' "${ENV_FILE}" "${NONINTERACTIVE}"
import os
import sys
from pathlib import Path

path = Path(sys.argv[1])
noninteractive = sys.argv[2].lower() == "true"

SECRET_HINT_KEYS = {"POSTGRES_PASSWORD", "ADMIN_PASSWORD", "JWT_SECRET", "JWE_SECRET", "GOOGLE_CLIENT_SECRET", "MAIL_PASSWORD", "GCP_ENCODED_CREDENTIALS", "FIREBASE_CREDENTIALS_BASE64", "FIREBASE_WEB_API_KEY", "FIREBASE_WEB_VAPID_KEY"}

lines = path.read_text(errors="ignore").splitlines()
values = {}
order = []
for line in lines:
    if line and not line.lstrip().startswith("#") and "=" in line:
        key, value = line.split("=", 1)
        values[key] = value
        if key not in order:
            order.append(key)

def put(key, value):
    values[key] = value
    if key not in order:
        order.append(key)

def get(key, default=""):
    return values.get(key, default)

def mask(key, value):
    if not value:
        return ""
    if key in SECRET_HINT_KEYS or any(token in key for token in ("PASSWORD", "SECRET", "TOKEN", "CREDENTIAL", "KEY")):
        return "<set>"
    return value

def ask(key, prompt, default=None, *, choices=None):
    current = get(key, "" if default is None else default)
    shown = mask(key, current)
    if noninteractive:
        print(f"{key}={shown or current} (kept)")
        put(key, current)
        return current
    suffix = f" [{shown}]" if shown else (f" [{default}]" if default not in (None, "") else "")
    if choices:
        suffix += f" ({'/'.join(choices)})"
    raw = input(f"{prompt}{suffix}: ").strip()
    value = current if raw == "" else raw
    if choices and value not in choices:
        raise SystemExit(f"{key}: expected one of {choices}, got {value!r}")
    put(key, value)
    return value

def section(title):
    print("\n== " + title + " ==")

section("1. 환경/GCP 범위 - 서버 생성 전 필수")
env = ask("ENVIRONMENT", "Environment", get("ENVIRONMENT") or "dev", choices=["dev", "prod"])
ask("PROJECT_ID", "GCP project id", get("PROJECT_ID") or "your-gcp-project-id")
ask("REGION", "GCP region", get("REGION") or "asia-northeast3")
ask("ZONE", "GCP zone", get("ZONE") or "asia-northeast3-a")
ask("NETWORK", "VPC network", get("NETWORK") or "default")
ask("SUBNET", "VPC subnet", get("SUBNET") or "default")

section("2. VM/디스크/IP - 서버 생성 전 필수")
ask("APP_INSTANCE_NAME", "App VM name", get("APP_INSTANCE_NAME") or f"gjlearn-{env}-app")
ask("DB_INSTANCE_NAME", "DB VM name", get("DB_INSTANCE_NAME") or f"gjlearn-{env}-db")
ask("APP_MACHINE_TYPE", "App machine type", get("APP_MACHINE_TYPE") or "e2-small")
ask("DB_MACHINE_TYPE", "DB machine type", get("DB_MACHINE_TYPE") or "e2-micro")
ask("BOOT_DISK_SIZE_GB", "Boot disk size GB", get("BOOT_DISK_SIZE_GB") or "10")
ask("BOOT_DISK_TYPE", "Boot disk type", get("BOOT_DISK_TYPE") or "pd-standard")
ask("IMAGE_FAMILY", "Image family", get("IMAGE_FAMILY") or "ubuntu-2204-lts")
ask("IMAGE_PROJECT", "Image project", get("IMAGE_PROJECT") or "ubuntu-os-cloud")
ask("APP_STATIC_IP_NAME", "App static external IP resource name", get("APP_STATIC_IP_NAME") or f"gjlearn-{env}-app-ip")
ask("DB_INTERNAL_IP_NAME", "DB reserved internal IP resource name", get("DB_INTERNAL_IP_NAME") or f"gjlearn-{env}-db-ip")
ask("DB_INTERNAL_IP", "Optional DB fixed internal IP; empty lets GCP choose", get("DB_INTERNAL_IP"))
ask("DB_EXTERNAL_IP_ENABLED", "Give DB VM an external IP?", get("DB_EXTERNAL_IP_ENABLED") or "false", choices=["true", "false"])
ask("APP_NETWORK_TAG", "App GCE network tag", get("APP_NETWORK_TAG") or f"gjlearn-{env}-app")
ask("DB_NETWORK_TAG", "DB GCE network tag", get("DB_NETWORK_TAG") or f"gjlearn-{env}-db")
ask("APP_BOOT_DISK_NAME", "App boot disk name", get("APP_BOOT_DISK_NAME") or f"gjlearn-{env}-app-boot")
ask("DB_BOOT_DISK_NAME", "DB boot disk name", get("DB_BOOT_DISK_NAME") or f"gjlearn-{env}-db-boot")

section("3. 방화벽/도메인 - 대부분 서버 생성 전 결정")
ask("SSH_SOURCE_RANGES", "SSH source CIDRs", get("SSH_SOURCE_RANGES") or ("YOUR_ADMIN_OR_GITHUB_RUNNER_IP/32" if env == "prod" else "0.0.0.0/0"))
ask("HTTP_SOURCE_RANGES", "HTTP/HTTPS source CIDRs", get("HTTP_SOURCE_RANGES") or "0.0.0.0/0")
ask("HTTP_PORTS", "Public HTTP ports", get("HTTP_PORTS") or "tcp:80,tcp:443")
api_domain = ask("API_DOMAIN", "API domain (empty allowed for raw IP dev)", get("API_DOMAIN"))
frontend_origin = ask("FRONTEND_ORIGIN", "Frontend origin", get("FRONTEND_ORIGIN") or (f"https://{api_domain}" if api_domain else "http://localhost:3000"))
ask("FRONTEND_BASE_URL", "Frontend base URL", get("FRONTEND_BASE_URL") or frontend_origin)
ask("FRONTEND_REDIRECT_URI", "Frontend OAuth redirect URI", get("FRONTEND_REDIRECT_URI") or frontend_origin.rstrip("/") + "/auth/google/callback")
ask("ENABLE_CADDY", "Install/configure Caddy reverse proxy on App VM?", get("ENABLE_CADDY") or "true", choices=["true", "false"])

section("4. Storage/GCS/IAM - 서버 생성 전 필수")
ask("APP_SERVICE_ACCOUNT_NAME", "App service account name", get("APP_SERVICE_ACCOUNT_NAME") or f"gjlearn-{env}-app")
ask("STORAGE_BUCKET_NAME", "GCS bucket name", get("STORAGE_BUCKET_NAME") or f"gjlearn-{env}-files")
ask("STORAGE_LOCATION", "GCS bucket location", get("STORAGE_LOCATION") or "asia-northeast3")
ask("STORAGE_PUBLIC_READ", "Make bucket public-read?", get("STORAGE_PUBLIC_READ") or "false", choices=["true", "false"])
ask("STORAGE_FOLDERS", "Comma-separated GCS folder prefixes", get("STORAGE_FOLDERS") or "profiles,editor,site-contents,documents/attachments,documents/purchase-items")

section("5. 배포 경로/OS 사용자 - 서버 생성 전 또는 install 전")
ask("SERVER_APP_DIR", "Remote app dir", get("SERVER_APP_DIR") or f"/home/min/app-{env}")
ask("SERVER_DB_DIR", "Remote DB dir", get("SERVER_DB_DIR") or f"/home/min/db-{env}")
ask("DEPLOY_OS_USER", "Remote deploy OS user", get("DEPLOY_OS_USER") or "min")
ask("SKIP_DB_INSTANCE", "Skip separate DB VM?", get("SKIP_DB_INSTANCE") or "false", choices=["true", "false"])

section("6. Tailscale - 서버 생성 후 인증 가능, 태그는 미리 기록 권장")
ask("APP_TAILSCALE_HOST", "Optional App Tailscale MagicDNS", get("APP_TAILSCALE_HOST"))
ask("DB_TAILSCALE_HOST", "Optional DB Tailscale MagicDNS", get("DB_TAILSCALE_HOST"))
ask("APP_TAILSCALE_TAGS", "App Tailscale tags", get("APP_TAILSCALE_TAGS") or f"tag:gjlearn-{env}-app,tag:gjlearn-app,tag:{env}")
ask("DB_TAILSCALE_TAGS", "DB Tailscale tags", get("DB_TAILSCALE_TAGS") or f"tag:gjlearn-{env}-db,tag:gjlearn-db,tag:{env}")
ask("APP_TAILSCALE_ACCEPT_DNS", "App Tailscale accept DNS?", get("APP_TAILSCALE_ACCEPT_DNS") or "false", choices=["true", "false"])
ask("DB_TAILSCALE_ACCEPT_DNS", "DB Tailscale accept DNS?", get("DB_TAILSCALE_ACCEPT_DNS") or "false", choices=["true", "false"])

section("7. Observability 이름 - 서버 생성 전/후 모두 수정 가능")
ask("CLOUD_LOGGING_LOG_ID", "Cloud Logging log id", get("CLOUD_LOGGING_LOG_ID") or f"gjlearn-{env}-app")
ask("CLOUD_LOGGING_WARN_ERROR_METRIC_NAME", "WARN/ERROR metric name", get("CLOUD_LOGGING_WARN_ERROR_METRIC_NAME") or f"gjlearn_{env}_app_warn_error_count")
ask("CLOUD_LOGGING_WARN_ERROR_POLICY_NAME", "WARN/ERROR alert policy name", get("CLOUD_LOGGING_WARN_ERROR_POLICY_NAME") or f"GJLearn-{env}-app-WARN-ERROR-logs")
ask("ALERT_NOTIFICATION_CHANNELS", "Optional comma-separated Monitoring notification channel resource names", get("ALERT_NOTIFICATION_CHANNELS"))

# Keep MailerSend defaults in the provisioning file so app env rendering inherits them.
section("8. Mail 기본값 - 실제 적용은 런타임 설정 단계")
ask("MAIL_ENABLED", "Default mail enabled", get("MAIL_ENABLED") or "false", choices=["true", "false"])
ask("MAIL_FALLBACK_TO_LOG", "Fallback mail failures to log", get("MAIL_FALLBACK_TO_LOG") or "false", choices=["true", "false"])
ask("MAIL_HOST", "SMTP host", get("MAIL_HOST") or "smtp.mailersend.net")
ask("MAIL_PORT", "SMTP port", get("MAIL_PORT") or "587")
ask("MAIL_USERNAME", "SMTP username", get("MAIL_USERNAME"))
ask("MAIL_PASSWORD", "SMTP password/API token", get("MAIL_PASSWORD"))
ask("MAIL_SMTP_AUTH", "SMTP auth", get("MAIL_SMTP_AUTH") or "true", choices=["true", "false"])
ask("MAIL_SMTP_STARTTLS_ENABLE", "SMTP STARTTLS", get("MAIL_SMTP_STARTTLS_ENABLE") or "true", choices=["true", "false"])
ask("MAIL_FROM_EMAIL", "Mail from email", get("MAIL_FROM_EMAIL") or "no-reply@example.com")
ask("MAIL_FROM_NAME", "Mail from name", get("MAIL_FROM_NAME") or "금정야학")
ask("MAIL_PASSWORD_RESET_PATH", "Password reset frontend path", get("MAIL_PASSWORD_RESET_PATH") or "/auth/reset-password")
ask("PASSWORD_RESET_EXPIRATION_MINUTES", "Password reset code expiration minutes", get("PASSWORD_RESET_EXPIRATION_MINUTES") or "15")
ask("MAIL_EMAIL_VERIFICATION_PATH", "Email verification frontend path", get("MAIL_EMAIL_VERIFICATION_PATH") or "/auth/email-verification")
ask("EMAIL_VERIFICATION_EXPIRATION_MINUTES", "Email verification code expiration minutes", get("EMAIL_VERIFICATION_EXPIRATION_MINUTES") or "15")

# Preserve comments and original ordering where possible.
out = []
seen = set()
for line in lines:
    if line and not line.lstrip().startswith("#") and "=" in line:
        key = line.split("=", 1)[0]
        if key in values:
            out.append(f"{key}={values[key]}")
            seen.add(key)
        else:
            out.append(line)
    else:
        out.append(line)
for key in order:
    if key not in seen:
        out.append(f"{key}={values[key]}")
path.write_text("\n".join(out).rstrip() + "\n")
path.chmod(0o600)
print(f"\nWrote {path}")
print("Next:")
print(f"  scripts/gcp/01_infra/01_provision-gcp.sh {path}")
print(f"  scripts/gcp/03_env_render/02_configure-runtime-env.sh {path}")
PY
