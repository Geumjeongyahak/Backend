#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/gcp/03_env_render/02_configure-runtime-env.sh scripts/gcp/00_env/dev.env [--local-only|--apply-app|--apply-db|--apply-all]

Interactive post-provision/runtime configuration editor.
Run after preinfra env exists. It renders app/db env files when missing, edits them interactively,
and can optionally copy them to the remote VMs and restart services.

Covers runtime values:
- DB runtime: PostgreSQL db/user/password, listen port, APP_DB_CIDR, exporter ports, Tailscale auth/tag values
- App runtime: frontend/API/OAuth URLs, DB connection, MailerSend SMTP, JWT/JWE, Firebase, GCP/Drive credentials,
  admin bootstrap, logging, Caddy toggle

Modes:
  --local-only  only write scripts/gcp/00_env/<env>.app.env and <env>.db.env (default)
  --apply-app   copy app env to App VM as ~/app-dev/.env and restart gjlearn-app
  --apply-db    copy db env to DB VM as ~/db-dev/.env and rerun DB install script when present
  --apply-all   apply both app and db

Environment overrides:
  APP_ENV_FILE=... DB_ENV_FILE=...
  USE_IAP_FOR_APP=true|false  USE_IAP_FOR_DB=true|false
  GJLEARN_NONINTERACTIVE=true
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

ENV_FILE="${1:?missing ENV_FILE. Try --help}"
MODE="${2:---local-only}"
case "${MODE}" in
  --local-only|--apply-app|--apply-db|--apply-all) ;;
  *) echo "unknown mode: ${MODE}" >&2; usage >&2; exit 1 ;;
esac

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "env file not found: ${ENV_FILE}" >&2
  exit 1
fi

ROOT_DIR="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
# shellcheck disable=SC1090
source "${ENV_FILE}"
: "${ENVIRONMENT:?missing ENVIRONMENT in ${ENV_FILE}}"
: "${PROJECT_ID:?missing PROJECT_ID in ${ENV_FILE}}"
: "${ZONE:?missing ZONE in ${ENV_FILE}}"
: "${APP_INSTANCE_NAME:?missing APP_INSTANCE_NAME in ${ENV_FILE}}"
: "${DB_INSTANCE_NAME:?missing DB_INSTANCE_NAME in ${ENV_FILE}}"

ENV_DIR="${ROOT_DIR}/scripts/gcp/00_env"
APP_ENV_FILE="${APP_ENV_FILE:-${ENV_DIR}/${ENVIRONMENT}.app.env}"
DB_ENV_FILE="${DB_ENV_FILE:-${ENV_DIR}/${ENVIRONMENT}.db.env}"
NONINTERACTIVE="${GJLEARN_NONINTERACTIVE:-false}"
USE_IAP_FOR_APP="${USE_IAP_FOR_APP:-false}"
USE_IAP_FOR_DB="${USE_IAP_FOR_DB:-true}"

mkdir -p "${ENV_DIR}"
if [[ ! -f "${DB_ENV_FILE}" ]]; then
  "${ROOT_DIR}/scripts/gcp/03_env_render/00_render-server-env.sh" "${ENV_FILE}" db > "${DB_ENV_FILE}"
  chmod 600 "${DB_ENV_FILE}"
  echo "rendered ${DB_ENV_FILE}"
fi
if [[ ! -f "${APP_ENV_FILE}" ]]; then
  "${ROOT_DIR}/scripts/gcp/03_env_render/00_render-server-env.sh" "${ENV_FILE}" app > "${APP_ENV_FILE}"
  chmod 600 "${APP_ENV_FILE}"
  echo "rendered ${APP_ENV_FILE}"
fi

python3 - <<'PY' "${ENV_FILE}" "${APP_ENV_FILE}" "${DB_ENV_FILE}" "${NONINTERACTIVE}"
import base64
import os
import secrets
import string
import sys
from pathlib import Path

env_file = Path(sys.argv[1])
app_file = Path(sys.argv[2])
db_file = Path(sys.argv[3])
noninteractive = sys.argv[4].lower() == "true"

SECRET_MARKERS = ("PASSWORD", "SECRET", "TOKEN", "CREDENTIAL", "KEY")

def parse(path):
    lines = path.read_text(errors="ignore").splitlines()
    values = {}
    order = []
    for line in lines:
        if line and not line.lstrip().startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            values[key] = value
            if key not in order:
                order.append(key)
    return lines, values, order

infra_lines, infra, _ = parse(env_file)
app_lines, app, app_order = parse(app_file)
db_lines, db, db_order = parse(db_file)

def random_base64(n=48):
    return base64.b64encode(secrets.token_bytes(n)).decode()

def random_password(n=28):
    alphabet = string.ascii_letters + string.digits + "!@#%^_-+="
    return "".join(secrets.choice(alphabet) for _ in range(n))

def mask(key, value):
    if not value:
        return ""
    if any(marker in key for marker in SECRET_MARKERS):
        return "<set>"
    return value

def put(values, order, key, value):
    values[key] = value
    if key not in order:
        order.append(key)

def get(values, key, default=""):
    return values.get(key, default)

def ask(values, order, key, prompt, default="", *, choices=None, secret=False):
    current = get(values, key, default)
    shown = mask(key, current) if secret or any(marker in key for marker in SECRET_MARKERS) else current
    if noninteractive:
        print(f"{key}={shown or current} (kept)")
        put(values, order, key, current)
        return current
    suffix = f" [{shown}]" if shown else (f" [{default}]" if default else "")
    if choices:
        suffix += f" ({'/'.join(choices)})"
    raw = input(f"{prompt}{suffix}: ").strip()
    value = current if raw == "" else raw
    if choices and value not in choices:
        raise SystemExit(f"{key}: expected one of {choices}, got {value!r}")
    put(values, order, key, value)
    return value

def ask_secret(values, order, key, prompt, default="", *, generator=None):
    current = get(values, key, default)
    placeholder = (current == "" or current.startswith("CHANGE_ME") or "YOUR_" in current or "example.com" in current)
    if noninteractive:
        if placeholder and generator:
            current = generator()
            print(f"{key}=<generated> (noninteractive default)")
        else:
            print(f"{key}={mask(key, current)} (kept)")
        put(values, order, key, current)
        return current
    suffix = " [<set>]" if current else ""
    gen_hint = ", blank/generate=random" if generator else ""
    raw = input(f"{prompt}{suffix}{gen_hint}: ").strip()
    if raw == "":
        if placeholder and generator:
            value = generator()
            print(f"  generated {key} ({len(value)} chars/base64 or password depending on key); value not printed")
        else:
            value = current
    elif generator and raw.lower() in {"g", "gen", "generate"}:
        value = generator()
        print(f"  generated {key} ({len(value)} chars/base64 or password depending on key); value not printed")
    else:
        value = raw
    put(values, order, key, value)
    return value

def section(title):
    print("\n== " + title + " ==")

env = infra.get("ENVIRONMENT", app.get("SPRING_PROFILES_ACTIVE", "dev"))
api_domain = infra.get("API_DOMAIN", app.get("API_DOMAIN", ""))
api_base = f"https://{api_domain}" if api_domain else ""
frontend_origin = infra.get("FRONTEND_ORIGIN", app.get("FRONTEND_BASE_URL", "http://localhost:3000"))
frontend_base = infra.get("FRONTEND_BASE_URL", app.get("FRONTEND_BASE_URL", frontend_origin))
frontend_redirect = infra.get("FRONTEND_REDIRECT_URI", app.get("FRONTEND_REDIRECT_URI", frontend_origin.rstrip("/") + "/auth/google/callback"))

def maybe_api_default():
    return (api_base + "/api/v1/auth/google/callback") if api_base else app.get("GOOGLE_REDIRECT_URI", "")

section("1. DB VM runtime - 서버 생성 후 DB install 전에 적용")
ask(db, db_order, "DB_PORT", "PostgreSQL port", get(db, "DB_PORT", "5432"))
ask(db, db_order, "DB_LISTEN_ADDRESS", "PostgreSQL listen_addresses", get(db, "DB_LISTEN_ADDRESS", "*"))
ask(db, db_order, "APP_DB_CIDR", "App VM CIDR allowed in pg_hba.conf", get(db, "APP_DB_CIDR", "100.64.0.0/10"))
ask(db, db_order, "NODE_EXPORTER_PORT", "DB node exporter port", get(db, "NODE_EXPORTER_PORT", "9100"))
ask(db, db_order, "POSTGRES_EXPORTER_PORT", "postgres exporter port", get(db, "POSTGRES_EXPORTER_PORT", "9187"))
ask(db, db_order, "POSTGRES_DB", "Database name", get(db, "POSTGRES_DB", "geumjeongyahak"))
ask(db, db_order, "POSTGRES_USER", "Database user", get(db, "POSTGRES_USER", "postgres"))
ask_secret(db, db_order, "POSTGRES_PASSWORD", "Database password", generator=random_password)
ask(db, db_order, "TAILSCALE_TAGS", "DB Tailscale tags", get(db, "TAILSCALE_TAGS", infra.get("DB_TAILSCALE_TAGS", "")))
ask(db, db_order, "TAILSCALE_ACCEPT_DNS", "DB Tailscale accept DNS?", get(db, "TAILSCALE_ACCEPT_DNS", infra.get("DB_TAILSCALE_ACCEPT_DNS", "false")), choices=["true", "false"])
ask_secret(db, db_order, "TAILSCALE_AUTHKEY", "Optional DB Tailscale auth key")

section("2. App DB 연결 - DB runtime 값과 일치해야 함")
ask(app, app_order, "SPRING_PROFILES_ACTIVE", "Spring profile", get(app, "SPRING_PROFILES_ACTIVE", env), choices=["dev", "prod"])
ask(app, app_order, "APP_PORT", "App HTTP port", get(app, "APP_PORT", "8080"))
ask(app, app_order, "MANAGEMENT_PORT", "Management/actuator port", get(app, "MANAGEMENT_PORT", get(app, "APP_PORT", "8080")))
ask(app, app_order, "POSTGRES_HOST", "DB host/IP from App VM", get(app, "POSTGRES_HOST", ""))
ask(app, app_order, "POSTGRES_PORT", "DB port from App VM", get(app, "POSTGRES_PORT", get(db, "DB_PORT", "5432")))
ask(app, app_order, "POSTGRES_DB", "DB name", get(app, "POSTGRES_DB", get(db, "POSTGRES_DB", "geumjeongyahak")))
ask(app, app_order, "POSTGRES_USER", "DB user", get(app, "POSTGRES_USER", get(db, "POSTGRES_USER", "postgres")))
ask_secret(app, app_order, "POSTGRES_PASSWORD", "DB password for app", default=get(db, "POSTGRES_PASSWORD", ""), generator=random_password)
ask(app, app_order, "POSTGRES_OPTIONS", "Optional JDBC query options", get(app, "POSTGRES_OPTIONS", ""))
ask(app, app_order, "FLYWAY_ENABLED", "Flyway enabled", get(app, "FLYWAY_ENABLED", "true"), choices=["true", "false"])
ask(app, app_order, "FLYWAY_BASELINE_ON_MIGRATE", "Flyway baseline-on-migrate", get(app, "FLYWAY_BASELINE_ON_MIGRATE", "false"), choices=["true", "false"])

section("3. Front/API 도메인, CORS, OAuth redirect")
ask(app, app_order, "API_DOMAIN", "API domain for Caddy", get(app, "API_DOMAIN", api_domain))
ask(app, app_order, "ENABLE_CADDY", "Caddy reverse proxy enabled", get(app, "ENABLE_CADDY", infra.get("ENABLE_CADDY", "true")), choices=["true", "false"])
ask(app, app_order, "FRONTEND_BASE_URL", "Frontend base URL", get(app, "FRONTEND_BASE_URL", frontend_base))
ask(app, app_order, "FRONTEND_REDIRECT_URI", "Frontend OAuth redirect URI", get(app, "FRONTEND_REDIRECT_URI", frontend_redirect))
ask(app, app_order, "CORS_ALLOWED_ORIGINS", "CORS allowed origins", get(app, "CORS_ALLOWED_ORIGINS", frontend_origin))
ask(app, app_order, "GOOGLE_REDIRECT_URI", "Backend Google OAuth callback URI", get(app, "GOOGLE_REDIRECT_URI", maybe_api_default()))
ask(app, app_order, "GOOGLE_CLIENT_ID", "Google OAuth client id", get(app, "GOOGLE_CLIENT_ID", ""), secret=True)
ask_secret(app, app_order, "GOOGLE_CLIENT_SECRET", "Google OAuth client secret")

section("4. JWT/JWE/Admin bootstrap")
ask_secret(app, app_order, "JWT_SECRET", "JWT secret", generator=random_base64)
ask_secret(app, app_order, "JWE_SECRET", "JWE secret", generator=random_base64)
ask(app, app_order, "JWT_ACCESS_EXP_SECONDS", "JWT access expiry seconds", get(app, "JWT_ACCESS_EXP_SECONDS", "1800"))
ask(app, app_order, "JWT_REFRESH_EXP_SECONDS", "JWT refresh expiry seconds", get(app, "JWT_REFRESH_EXP_SECONDS", "1209600"))
ask(app, app_order, "JWT_OAUTH2_TEMP_EXP_SECONDS", "OAuth temp token expiry seconds", get(app, "JWT_OAUTH2_TEMP_EXP_SECONDS", "300"))
ask(app, app_order, "ADMIN_BOOTSTRAP_ENABLED", "Bootstrap admin account?", get(app, "ADMIN_BOOTSTRAP_ENABLED", "true"), choices=["true", "false"])
ask(app, app_order, "ADMIN_EMAIL", "Admin email", get(app, "ADMIN_EMAIL", "admin@example.com"))
ask_secret(app, app_order, "ADMIN_PASSWORD", "Admin password", generator=random_password)
ask(app, app_order, "ADMIN_NAME", "Admin name", get(app, "ADMIN_NAME", "관리자"))

section("5. MailerSend/SMTP")
ask(app, app_order, "MAIL_ENABLED", "Mail enabled", get(app, "MAIL_ENABLED", infra.get("MAIL_ENABLED", "false")), choices=["true", "false"])
ask(app, app_order, "MAIL_FALLBACK_TO_LOG", "Fallback mail failures to log", get(app, "MAIL_FALLBACK_TO_LOG", infra.get("MAIL_FALLBACK_TO_LOG", "false")), choices=["true", "false"])
ask(app, app_order, "MAIL_HOST", "SMTP host", get(app, "MAIL_HOST", infra.get("MAIL_HOST", "smtp.mailersend.net")))
ask(app, app_order, "MAIL_PORT", "SMTP port", get(app, "MAIL_PORT", infra.get("MAIL_PORT", "587")))
ask(app, app_order, "MAIL_USERNAME", "SMTP username", get(app, "MAIL_USERNAME", infra.get("MAIL_USERNAME", "")), secret=True)
ask_secret(app, app_order, "MAIL_PASSWORD", "SMTP password/API token", default=infra.get("MAIL_PASSWORD", ""))
ask(app, app_order, "MAIL_SMTP_AUTH", "SMTP auth", get(app, "MAIL_SMTP_AUTH", infra.get("MAIL_SMTP_AUTH", "true")), choices=["true", "false"])
ask(app, app_order, "MAIL_SMTP_STARTTLS_ENABLE", "SMTP STARTTLS", get(app, "MAIL_SMTP_STARTTLS_ENABLE", infra.get("MAIL_SMTP_STARTTLS_ENABLE", "true")), choices=["true", "false"])
ask(app, app_order, "MAIL_FROM_EMAIL", "Mail from email", get(app, "MAIL_FROM_EMAIL", infra.get("MAIL_FROM_EMAIL", "no-reply@example.com")))
ask(app, app_order, "MAIL_FROM_NAME", "Mail from name", get(app, "MAIL_FROM_NAME", infra.get("MAIL_FROM_NAME", "금정야학")))
ask(app, app_order, "MAIL_PASSWORD_RESET_PATH", "Password reset frontend path", get(app, "MAIL_PASSWORD_RESET_PATH", infra.get("MAIL_PASSWORD_RESET_PATH", "/auth/reset-password")))
ask(app, app_order, "PASSWORD_RESET_EXPIRATION_MINUTES", "Password reset code expiration minutes", get(app, "PASSWORD_RESET_EXPIRATION_MINUTES", infra.get("PASSWORD_RESET_EXPIRATION_MINUTES", "15")))
ask(app, app_order, "MAIL_EMAIL_VERIFICATION_PATH", "Email verification frontend path", get(app, "MAIL_EMAIL_VERIFICATION_PATH", infra.get("MAIL_EMAIL_VERIFICATION_PATH", "/auth/email-verification")))
ask(app, app_order, "EMAIL_VERIFICATION_EXPIRATION_MINUTES", "Email verification code expiration minutes", get(app, "EMAIL_VERIFICATION_EXPIRATION_MINUTES", infra.get("EMAIL_VERIFICATION_EXPIRATION_MINUTES", "15")))

section("6. GCP Storage credentials")
ask(app, app_order, "GCP_PROJECT_ID", "GCP project id", get(app, "GCP_PROJECT_ID", infra.get("PROJECT_ID", "")))
ask(app, app_order, "GCP_PROD_BUCKET_NAME", "Prod bucket env value", get(app, "GCP_PROD_BUCKET_NAME", infra.get("STORAGE_BUCKET_NAME", "")))
ask(app, app_order, "GCP_DEV_BUCKET_NAME", "Dev bucket env value", get(app, "GCP_DEV_BUCKET_NAME", infra.get("STORAGE_BUCKET_NAME", "")))
ask_secret(app, app_order, "GCP_ENCODED_CREDENTIALS", "Optional base64 service-account JSON; leave empty if VM metadata credentials are enough")

section("7. Google Drive direct upload")
ask(app, app_order, "GOOGLE_DRIVE_SHARED_DRIVE_ID", "Shared Drive id", get(app, "GOOGLE_DRIVE_SHARED_DRIVE_ID", infra.get("GOOGLE_DRIVE_SHARED_DRIVE_ID", "")))
ask(app, app_order, "GOOGLE_DRIVE_MAKE_LINK_PUBLIC", "Make uploaded Drive files link-public?", get(app, "GOOGLE_DRIVE_MAKE_LINK_PUBLIC", infra.get("GOOGLE_DRIVE_MAKE_LINK_PUBLIC", "true")), choices=["true", "false"])
ask(app, app_order, "GOOGLE_DRIVE_OAUTH_CLIENT_ID", "Drive OAuth client id", get(app, "GOOGLE_DRIVE_OAUTH_CLIENT_ID", infra.get("GOOGLE_DRIVE_OAUTH_CLIENT_ID", "")), secret=True)
ask_secret(app, app_order, "GOOGLE_DRIVE_OAUTH_CLIENT_SECRET", "Drive OAuth client secret")
ask_secret(app, app_order, "GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN", "Drive OAuth refresh token")
ask(app, app_order, "GOOGLE_DRIVE_FOLDER_ID_HANDOVER", "Drive folder id for handover", get(app, "GOOGLE_DRIVE_FOLDER_ID_HANDOVER", infra.get("GOOGLE_DRIVE_FOLDER_ID_HANDOVER", "")))
ask(app, app_order, "GOOGLE_DRIVE_FOLDER_ID_EXAM_MATERIALS", "Drive folder id for examMaterials", get(app, "GOOGLE_DRIVE_FOLDER_ID_EXAM_MATERIALS", infra.get("GOOGLE_DRIVE_FOLDER_ID_EXAM_MATERIALS", "")))
ask(app, app_order, "GOOGLE_DRIVE_FOLDER_ID_DOCUMENT_FORMS", "Drive folder id for documentForms", get(app, "GOOGLE_DRIVE_FOLDER_ID_DOCUMENT_FORMS", infra.get("GOOGLE_DRIVE_FOLDER_ID_DOCUMENT_FORMS", "")))
ask(app, app_order, "GOOGLE_DRIVE_FOLDER_ID_MEETING_RECORDS", "Drive folder id for meetingRecords", get(app, "GOOGLE_DRIVE_FOLDER_ID_MEETING_RECORDS", infra.get("GOOGLE_DRIVE_FOLDER_ID_MEETING_RECORDS", "")))
ask(app, app_order, "GOOGLE_DRIVE_FOLDER_ID_BOARD", "Drive folder id for board", get(app, "GOOGLE_DRIVE_FOLDER_ID_BOARD", infra.get("GOOGLE_DRIVE_FOLDER_ID_BOARD", "")))

section("8. Firebase")
ask(app, app_order, "FIREBASE_ENABLED", "Firebase enabled", get(app, "FIREBASE_ENABLED", "false"), choices=["true", "false"])
ask(app, app_order, "FIREBASE_PROJECT_ID", "Firebase project id", get(app, "FIREBASE_PROJECT_ID", ""))
ask_secret(app, app_order, "FIREBASE_CREDENTIALS_BASE64", "Firebase service-account JSON base64")
ask(app, app_order, "FIREBASE_WEB_API_KEY", "Firebase web API key", get(app, "FIREBASE_WEB_API_KEY", ""), secret=True)
ask(app, app_order, "FIREBASE_WEB_AUTH_DOMAIN", "Firebase web auth domain", get(app, "FIREBASE_WEB_AUTH_DOMAIN", ""))
ask(app, app_order, "FIREBASE_WEB_PROJECT_ID", "Firebase web project id", get(app, "FIREBASE_WEB_PROJECT_ID", get(app, "FIREBASE_PROJECT_ID", "")))
ask(app, app_order, "FIREBASE_WEB_STORAGE_BUCKET", "Firebase web storage bucket", get(app, "FIREBASE_WEB_STORAGE_BUCKET", ""))
ask(app, app_order, "FIREBASE_WEB_MESSAGING_SENDER_ID", "Firebase messaging sender id", get(app, "FIREBASE_WEB_MESSAGING_SENDER_ID", ""))
ask(app, app_order, "FIREBASE_WEB_APP_ID", "Firebase web app id", get(app, "FIREBASE_WEB_APP_ID", ""))
ask_secret(app, app_order, "FIREBASE_WEB_VAPID_KEY", "Firebase web VAPID key")

section("9. App Tailscale/logging")
ask(app, app_order, "TAILSCALE_TAGS", "App Tailscale tags", get(app, "TAILSCALE_TAGS", infra.get("APP_TAILSCALE_TAGS", "")))
ask(app, app_order, "TAILSCALE_ACCEPT_DNS", "App Tailscale accept DNS?", get(app, "TAILSCALE_ACCEPT_DNS", infra.get("APP_TAILSCALE_ACCEPT_DNS", "false")), choices=["true", "false"])
ask_secret(app, app_order, "TAILSCALE_AUTHKEY", "Optional App Tailscale auth key")
ask(app, app_order, "APP_LOG_DIR", "App log dir", get(app, "APP_LOG_DIR", "./logs/app"))
ask(app, app_order, "LOG_FILE_PATTERN", "Logback rolling file pattern", get(app, "LOG_FILE_PATTERN", "./logs/app/application.%d{yyyy-MM-dd}.log"))
ask(app, app_order, "LOG_UPLOAD_PATH", "Cloud Ops Agent log upload glob", get(app, "LOG_UPLOAD_PATH", "./logs/app/application.*.log"))
ask(app, app_order, "LOG_FILE_MAX_HISTORY", "Log max history days", get(app, "LOG_FILE_MAX_HISTORY", "30"))
ask(app, app_order, "LOG_FILE_TOTAL_SIZE_CAP", "Log total size cap", get(app, "LOG_FILE_TOTAL_SIZE_CAP", "1GB"))
ask(app, app_order, "CLOUD_LOGGING_ENABLED", "Cloud logging enabled", get(app, "CLOUD_LOGGING_ENABLED", "true"), choices=["true", "false"])
ask(app, app_order, "CLOUD_LOGGING_LOG_ID", "Cloud Logging log id", get(app, "CLOUD_LOGGING_LOG_ID", infra.get("CLOUD_LOGGING_LOG_ID", f"gjlearn-{env}-app")))

def write_env(path, original_lines, values, order):
    out=[]
    seen=set()
    for line in original_lines:
        if line and not line.lstrip().startswith("#") and "=" in line:
            key=line.split("=",1)[0]
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
    path.write_text("\n".join(out).rstrip()+"\n")
    path.chmod(0o600)

write_env(app_file, app_lines, app, app_order)
write_env(db_file, db_lines, db, db_order)
print(f"\nWrote {app_file}")
print(f"Wrote {db_file}")
print("Secrets were not printed. Keep these files out of git.")
PY

run_scp() {
  local use_iap="$1"
  shift
  local args=(--project "${PROJECT_ID}" --zone "${ZONE}")
  if [[ "${use_iap}" == "true" ]]; then
    args+=(--tunnel-through-iap)
  fi
  gcloud compute scp "$@" "${args[@]}"
}

run_ssh() {
  local instance="$1"
  local use_iap="$2"
  shift 2
  local args=(--project "${PROJECT_ID}" --zone "${ZONE}")
  if [[ "${use_iap}" == "true" ]]; then
    args+=(--tunnel-through-iap)
  fi
  gcloud compute ssh "${instance}" "${args[@]}" --command "$*"
}

apply_db() {
  if [[ "${SKIP_DB_INSTANCE:-false}" == "true" ]]; then
    echo "SKIP_DB_INSTANCE=true; skipping DB remote apply"
    return 0
  fi
  echo "Applying DB env to ${DB_INSTANCE_NAME}:~/db-dev/.env"
  run_scp "${USE_IAP_FOR_DB}" "${DB_ENV_FILE}" "${DB_INSTANCE_NAME}:~/db-dev/$(basename "${DB_ENV_FILE}")"
  run_ssh "${DB_INSTANCE_NAME}" "${USE_IAP_FOR_DB}" \
    "cd ~/db-dev && mv '$(basename "${DB_ENV_FILE}")' .env && chmod 600 .env && if [ -x ./01_install-db-service.sh ]; then ./01_install-db-service.sh; else echo 'DB env updated; 01_install-db-service.sh not found/executable, restart DB manually if needed'; fi"
}

apply_app() {
  echo "Applying App env to ${APP_INSTANCE_NAME}:~/app-dev/.env"
  run_scp "${USE_IAP_FOR_APP}" "${APP_ENV_FILE}" "${APP_INSTANCE_NAME}:~/app-dev/$(basename "${APP_ENV_FILE}")"
  run_ssh "${APP_INSTANCE_NAME}" "${USE_IAP_FOR_APP}" \
    "cd ~/app-dev && mv '$(basename "${APP_ENV_FILE}")' .env && chmod 600 .env && if systemctl list-unit-files | grep -q '^gjlearn-app.service'; then sudo systemctl restart gjlearn-app && sleep 3 && sudo systemctl is-active --quiet gjlearn-app && curl -fsS http://127.0.0.1:\${APP_PORT:-8080}/actuator/health; else echo 'App env updated; gjlearn-app.service not installed yet'; fi"
}

case "${MODE}" in
  --local-only)
    cat <<EOF
Local runtime env update complete.
- ${APP_ENV_FILE}
- ${DB_ENV_FILE}

To apply later:
  scripts/gcp/03_env_render/02_configure-runtime-env.sh ${ENV_FILE} --apply-app
  scripts/gcp/03_env_render/02_configure-runtime-env.sh ${ENV_FILE} --apply-db
EOF
    ;;
  --apply-app)
    apply_app
    ;;
  --apply-db)
    apply_db
    ;;
  --apply-all)
    apply_db
    apply_app
    ;;
esac
