#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/gcp/03_env_render/03_configure-app-runtime-section.sh scripts/gcp/00_env/dev.env SECTION [--local-only|--apply-app]

SECTION values:
  frontend     API_DOMAIN, Caddy, CORS, frontend base/redirect URLs
  db           App-side DB connection and Flyway values
  security     JWT/JWE secrets and admin bootstrap
  mailersend   MailerSend/SMTP values
  oauth        Google OAuth values; can import client_id/client_secret from Google client JSON
  firebase     Firebase values; can base64-encode service-account JSON from a file path
  gcp-storage  GCP storage bucket/project values; can base64-encode service-account JSON from a file path
  google-drive Google Drive Shared Drive/folder ids for backend direct upload
  logging      App log/Cloud Ops Agent values
  tailscale    App Tailscale tags/auth key
  all          Run all sections

Modes:
  --local-only  update scripts/gcp/00_env/<env>.app.env only (default)
  --apply-app   copy app env to App VM as ~/app-dev/.env and restart gjlearn-app if installed

Blank/default behavior:
- Existing non-placeholder values are kept when you press Enter.
- Empty or CHANGE_ME JWT/JWE/Admin password values are generated when you press Enter.
- Firebase/GCP credential prompts accept a file path and store one-line base64.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

ENV_FILE="${1:?missing ENV_FILE}"
SECTION="${2:?missing SECTION}"
MODE="${3:---local-only}"
case "${SECTION}" in frontend|db|security|mailersend|oauth|firebase|gcp-storage|google-drive|logging|tailscale|all) ;;
  *) echo "unknown SECTION: ${SECTION}" >&2; usage >&2; exit 1 ;;
esac
case "${MODE}" in --local-only|--apply-app) ;;
  *) echo "unknown mode: ${MODE}" >&2; usage >&2; exit 1 ;;
esac

ROOT_DIR="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
if [[ ! -f "${ENV_FILE}" ]]; then
  echo "env file not found: ${ENV_FILE}" >&2
  exit 1
fi
# shellcheck disable=SC1090
source "${ENV_FILE}"
: "${ENVIRONMENT:?missing ENVIRONMENT in ${ENV_FILE}}"
: "${PROJECT_ID:?missing PROJECT_ID in ${ENV_FILE}}"
: "${ZONE:?missing ZONE in ${ENV_FILE}}"
: "${APP_INSTANCE_NAME:?missing APP_INSTANCE_NAME in ${ENV_FILE}}"

ENV_DIR="${ROOT_DIR}/scripts/gcp/00_env"
APP_ENV_FILE="${APP_ENV_FILE:-${ENV_DIR}/${ENVIRONMENT}.app.env}"
USE_IAP_FOR_APP="${USE_IAP_FOR_APP:-false}"
NONINTERACTIVE="${GJLEARN_NONINTERACTIVE:-false}"

mkdir -p "${ENV_DIR}"
if [[ ! -f "${APP_ENV_FILE}" ]]; then
  "${ROOT_DIR}/scripts/gcp/03_env_render/00_render-server-env.sh" "${ENV_FILE}" app > "${APP_ENV_FILE}"
  chmod 600 "${APP_ENV_FILE}"
  echo "rendered ${APP_ENV_FILE}"
fi

python3 - <<'PY' "${ENV_FILE}" "${APP_ENV_FILE}" "${SECTION}" "${NONINTERACTIVE}"
import base64, json, secrets, string, sys
from pathlib import Path

env_path=Path(sys.argv[1])
app_path=Path(sys.argv[2])
section=sys.argv[3]
noninteractive=sys.argv[4].lower()=="true"
SECRET_MARKERS=("PASSWORD","SECRET","TOKEN","CREDENTIAL","KEY")

def parse(path):
    lines=path.read_text(errors="ignore").splitlines()
    values={}; order=[]
    for line in lines:
        if line and not line.lstrip().startswith('#') and '=' in line:
            k,v=line.split('=',1); values[k]=v
            if k not in order: order.append(k)
    return lines, values, order

infra_lines, infra, _ = parse(env_path)
app_lines, app, order = parse(app_path)

def put(k,v):
    app[k]=v
    if k not in order: order.append(k)

def get(k, default=""):
    return app.get(k, infra.get(k, default))

def is_secret(k):
    return any(m in k for m in SECRET_MARKERS)

def is_placeholder(v):
    return v == "" or v.startswith("CHANGE_ME") or "YOUR_" in v or "example.com" in v

def mask(k,v):
    return "<set>" if v and is_secret(k) else v

def rand_b64(n=48): return base64.b64encode(secrets.token_bytes(n)).decode()
def rand_pw(n=28):
    alphabet=string.ascii_letters+string.digits+"!@#%^_-+="
    return ''.join(secrets.choice(alphabet) for _ in range(n))

def ask(k, prompt, default="", choices=None, secret=False):
    cur=get(k, default)
    shown=mask(k,cur) if secret or is_secret(k) else cur
    if noninteractive:
        put(k, cur); print(f"{k}={shown or cur} (kept)"); return cur
    suffix=f" [{shown}]" if shown else (f" [{default}]" if default else "")
    if choices: suffix += f" ({'/'.join(choices)})"
    raw=input(f"{prompt}{suffix}: ").strip()
    val=cur if raw=="" else raw
    if choices and val not in choices:
        raise SystemExit(f"{k}: expected one of {choices}, got {val!r}")
    put(k,val); return val

def ask_secret(k, prompt, default="", generator=None):
    cur=get(k, default)
    if noninteractive:
        if generator and is_placeholder(cur):
            cur=generator(); print(f"{k}=<generated>")
        else:
            print(f"{k}={mask(k,cur)} (kept)")
        put(k,cur); return cur
    suffix=" [<set>]" if cur else ""
    hint=", blank/generate=random" if generator else ""
    raw=input(f"{prompt}{suffix}{hint}: ").strip()
    if raw=="" and generator and is_placeholder(cur):
        val=generator(); print(f"  generated {k}; value not printed")
    elif generator and raw.lower() in {"g","gen","generate"}:
        val=generator(); print(f"  generated {k}; value not printed")
    else:
        val=cur if raw=="" else raw
    put(k,val); return val

def file_to_b64(path):
    p=Path(path).expanduser()
    if not p.is_file(): raise SystemExit(f"file not found: {p}")
    return base64.b64encode(p.read_bytes()).decode()

def ask_b64_or_file(k, prompt):
    cur=get(k,"")
    if noninteractive:
        put(k,cur); print(f"{k}={mask(k,cur)} (kept)"); return cur
    raw=input(f"{prompt} [<set> if already set; enter file path, raw base64, or blank keep]: ").strip()
    if raw=="": val=cur
    elif Path(raw).expanduser().is_file():
        val=file_to_b64(raw); print(f"  encoded {raw} into {k}; value not printed")
    else: val=raw
    put(k,val); return val

def import_google_client_json():
    if noninteractive: return
    raw=input("Optional Google OAuth client JSON path (blank skip): ").strip()
    if not raw: return
    p=Path(raw).expanduser()
    data=json.loads(p.read_text())
    client=(data.get('web') or data.get('installed') or data)
    if client.get('client_id'): put('GOOGLE_CLIENT_ID', client['client_id'])
    if client.get('client_secret'): put('GOOGLE_CLIENT_SECRET', client['client_secret'])
    redirects=client.get('redirect_uris') or []
    if redirects and not get('GOOGLE_REDIRECT_URI'):
        put('GOOGLE_REDIRECT_URI', redirects[0])
    print("  imported GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET from JSON; secret not printed")

def title(t): print("\n== "+t+" ==")

def frontend():
    title("Frontend/API/CORS")
    api=ask('API_DOMAIN','API domain for Caddy', get('API_DOMAIN', infra.get('API_DOMAIN','')))
    ask('ENABLE_CADDY','Caddy reverse proxy enabled', get('ENABLE_CADDY', infra.get('ENABLE_CADDY','true')), ['true','false'])
    front=ask('FRONTEND_BASE_URL','Frontend base URL', get('FRONTEND_BASE_URL', infra.get('FRONTEND_BASE_URL', infra.get('FRONTEND_ORIGIN','http://localhost:3000'))))
    ask('FRONTEND_REDIRECT_URI','Frontend OAuth redirect URI', get('FRONTEND_REDIRECT_URI', infra.get('FRONTEND_REDIRECT_URI', front.rstrip('/')+'/auth/google/callback')))
    ask('CORS_ALLOWED_ORIGINS','CORS allowed origins', get('CORS_ALLOWED_ORIGINS', infra.get('FRONTEND_ORIGIN',front)))
    default_cb=(f"https://{api}/api/v1/auth/google/callback" if api else get('GOOGLE_REDIRECT_URI',''))
    ask('GOOGLE_REDIRECT_URI','Backend Google OAuth callback URI', get('GOOGLE_REDIRECT_URI', default_cb))

def db():
    title("App DB connection")
    ask('POSTGRES_HOST','DB host/IP from App VM', get('POSTGRES_HOST',''))
    ask('POSTGRES_PORT','DB port from App VM', get('POSTGRES_PORT','5432'))
    ask('POSTGRES_DB','DB name', get('POSTGRES_DB','geumjeongyahak'))
    ask('POSTGRES_USER','DB user', get('POSTGRES_USER','postgres'))
    ask_secret('POSTGRES_PASSWORD','DB password for app', generator=rand_pw)
    ask('POSTGRES_OPTIONS','Optional JDBC query options', get('POSTGRES_OPTIONS',''))
    ask('FLYWAY_ENABLED','Flyway enabled', get('FLYWAY_ENABLED','true'), ['true','false'])
    ask('FLYWAY_BASELINE_ON_MIGRATE','Flyway baseline-on-migrate', get('FLYWAY_BASELINE_ON_MIGRATE','false'), ['true','false'])

def security():
    title("JWT/JWE/Admin")
    ask_secret('JWT_SECRET','JWT secret', generator=rand_b64)
    ask_secret('JWE_SECRET','JWE secret', generator=rand_b64)
    ask('JWT_ACCESS_EXP_SECONDS','JWT access expiry seconds', get('JWT_ACCESS_EXP_SECONDS','1800'))
    ask('JWT_REFRESH_EXP_SECONDS','JWT refresh expiry seconds', get('JWT_REFRESH_EXP_SECONDS','1209600'))
    ask('JWT_OAUTH2_TEMP_EXP_SECONDS','OAuth temp token expiry seconds', get('JWT_OAUTH2_TEMP_EXP_SECONDS','300'))
    ask('ADMIN_BOOTSTRAP_ENABLED','Bootstrap admin account?', get('ADMIN_BOOTSTRAP_ENABLED','true'), ['true','false'])
    ask('ADMIN_EMAIL','Admin email', get('ADMIN_EMAIL','admin@example.com'))
    ask_secret('ADMIN_PASSWORD','Admin password', generator=rand_pw)
    ask('ADMIN_NAME','Admin name', get('ADMIN_NAME','관리자'))

def mailersend():
    title("MailerSend/SMTP")
    ask('MAIL_ENABLED','Mail enabled', get('MAIL_ENABLED','false'), ['true','false'])
    ask('MAIL_FALLBACK_TO_LOG','Fallback mail failures to log', get('MAIL_FALLBACK_TO_LOG','false'), ['true','false'])
    ask('MAIL_HOST','SMTP host', get('MAIL_HOST','smtp.mailersend.net'))
    ask('MAIL_PORT','SMTP port', get('MAIL_PORT','587'))
    ask('MAIL_USERNAME','SMTP username', get('MAIL_USERNAME',''), secret=True)
    ask_secret('MAIL_PASSWORD','SMTP password/API token')
    ask('MAIL_SMTP_AUTH','SMTP auth', get('MAIL_SMTP_AUTH','true'), ['true','false'])
    ask('MAIL_SMTP_STARTTLS_ENABLE','SMTP STARTTLS', get('MAIL_SMTP_STARTTLS_ENABLE','true'), ['true','false'])
    ask('MAIL_FROM_EMAIL','Mail from email', get('MAIL_FROM_EMAIL','no-reply@example.com'))
    ask('MAIL_FROM_NAME','Mail from name', get('MAIL_FROM_NAME','금정야학'))
    ask('MAIL_PASSWORD_RESET_PATH','Password reset frontend path', get('MAIL_PASSWORD_RESET_PATH','/auth/reset-password'))
    ask('PASSWORD_RESET_EXPIRATION_MINUTES','Password reset code expiration minutes', get('PASSWORD_RESET_EXPIRATION_MINUTES','15'))
    ask('MAIL_EMAIL_VERIFICATION_PATH','Email verification frontend path', get('MAIL_EMAIL_VERIFICATION_PATH','/auth/email-verification'))
    ask('EMAIL_VERIFICATION_EXPIRATION_MINUTES','Email verification code expiration minutes', get('EMAIL_VERIFICATION_EXPIRATION_MINUTES','15'))

def oauth():
    title("Google OAuth")
    import_google_client_json()
    ask('GOOGLE_CLIENT_ID','Google OAuth client id', get('GOOGLE_CLIENT_ID',''), secret=True)
    ask_secret('GOOGLE_CLIENT_SECRET','Google OAuth client secret')
    frontend()

def firebase():
    title("Firebase")
    ask('FIREBASE_ENABLED','Firebase enabled', get('FIREBASE_ENABLED','false'), ['true','false'])
    ask('FIREBASE_PROJECT_ID','Firebase project id', get('FIREBASE_PROJECT_ID',''))
    ask_b64_or_file('FIREBASE_CREDENTIALS_BASE64','Firebase service-account JSON')
    ask('FIREBASE_WEB_API_KEY','Firebase web API key', get('FIREBASE_WEB_API_KEY',''), secret=True)
    ask('FIREBASE_WEB_AUTH_DOMAIN','Firebase web auth domain', get('FIREBASE_WEB_AUTH_DOMAIN',''))
    ask('FIREBASE_WEB_PROJECT_ID','Firebase web project id', get('FIREBASE_WEB_PROJECT_ID', get('FIREBASE_PROJECT_ID','')))
    ask('FIREBASE_WEB_STORAGE_BUCKET','Firebase web storage bucket', get('FIREBASE_WEB_STORAGE_BUCKET',''))
    ask('FIREBASE_WEB_MESSAGING_SENDER_ID','Firebase messaging sender id', get('FIREBASE_WEB_MESSAGING_SENDER_ID',''))
    ask('FIREBASE_WEB_APP_ID','Firebase web app id', get('FIREBASE_WEB_APP_ID',''))
    ask_secret('FIREBASE_WEB_VAPID_KEY','Firebase web VAPID key')

def gcp_storage():
    title("GCP Storage")
    ask('GCP_PROJECT_ID','GCP project id', get('GCP_PROJECT_ID', infra.get('PROJECT_ID','')))
    ask('GCP_PROD_BUCKET_NAME','Prod bucket env value', get('GCP_PROD_BUCKET_NAME', infra.get('STORAGE_BUCKET_NAME','')))
    ask('GCP_DEV_BUCKET_NAME','Dev bucket env value', get('GCP_DEV_BUCKET_NAME', infra.get('STORAGE_BUCKET_NAME','')))
    ask_b64_or_file('GCP_ENCODED_CREDENTIALS','GCP service-account JSON')

def google_drive():
    title("Google Drive direct upload")
    ask('GOOGLE_DRIVE_SHARED_DRIVE_ID','Shared Drive id', get('GOOGLE_DRIVE_SHARED_DRIVE_ID', infra.get('GOOGLE_DRIVE_SHARED_DRIVE_ID','')))
    ask('GOOGLE_DRIVE_MAKE_LINK_PUBLIC','Make uploaded Drive files link-public?', get('GOOGLE_DRIVE_MAKE_LINK_PUBLIC', infra.get('GOOGLE_DRIVE_MAKE_LINK_PUBLIC','true')), ['true','false'])
    ask('GOOGLE_DRIVE_OAUTH_CLIENT_ID','Drive OAuth client id', get('GOOGLE_DRIVE_OAUTH_CLIENT_ID', infra.get('GOOGLE_DRIVE_OAUTH_CLIENT_ID','')), secret=True)
    ask_secret('GOOGLE_DRIVE_OAUTH_CLIENT_SECRET','Drive OAuth client secret')
    ask_secret('GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN','Drive OAuth refresh token')
    ask('GOOGLE_DRIVE_FOLDER_ID_HANDOVER','Drive folder id for handover', get('GOOGLE_DRIVE_FOLDER_ID_HANDOVER', infra.get('GOOGLE_DRIVE_FOLDER_ID_HANDOVER','')))
    ask('GOOGLE_DRIVE_FOLDER_ID_EXAM_MATERIALS','Drive folder id for examMaterials', get('GOOGLE_DRIVE_FOLDER_ID_EXAM_MATERIALS', infra.get('GOOGLE_DRIVE_FOLDER_ID_EXAM_MATERIALS','')))
    ask('GOOGLE_DRIVE_FOLDER_ID_DOCUMENT_FORMS','Drive folder id for documentForms', get('GOOGLE_DRIVE_FOLDER_ID_DOCUMENT_FORMS', infra.get('GOOGLE_DRIVE_FOLDER_ID_DOCUMENT_FORMS','')))
    ask('GOOGLE_DRIVE_FOLDER_ID_MEETING_RECORDS','Drive folder id for meetingRecords', get('GOOGLE_DRIVE_FOLDER_ID_MEETING_RECORDS', infra.get('GOOGLE_DRIVE_FOLDER_ID_MEETING_RECORDS','')))
    ask('GOOGLE_DRIVE_FOLDER_ID_BOARD','Drive folder id for board', get('GOOGLE_DRIVE_FOLDER_ID_BOARD', infra.get('GOOGLE_DRIVE_FOLDER_ID_BOARD','')))

def logging():
    title("Logging")
    ask('APP_LOG_DIR','App log dir', get('APP_LOG_DIR','./logs/app'))
    ask('LOG_FILE_PATTERN','Logback rolling file pattern', get('LOG_FILE_PATTERN','./logs/app/application.%d{yyyy-MM-dd}.log'))
    ask('LOG_UPLOAD_PATH','Cloud Ops Agent log upload glob', get('LOG_UPLOAD_PATH','./logs/app/application.*.log'))
    ask('LOG_FILE_MAX_HISTORY','Log max history days', get('LOG_FILE_MAX_HISTORY','30'))
    ask('LOG_FILE_TOTAL_SIZE_CAP','Log total size cap', get('LOG_FILE_TOTAL_SIZE_CAP','1GB'))
    ask('CLOUD_LOGGING_ENABLED','Cloud logging enabled', get('CLOUD_LOGGING_ENABLED','true'), ['true','false'])
    ask('CLOUD_LOGGING_LOG_ID','Cloud Logging log id', get('CLOUD_LOGGING_LOG_ID', infra.get('CLOUD_LOGGING_LOG_ID', f"gjlearn-{infra.get('ENVIRONMENT','app')}-app")))

def tailscale():
    title("App Tailscale")
    ask('TAILSCALE_TAGS','App Tailscale tags', get('TAILSCALE_TAGS', infra.get('APP_TAILSCALE_TAGS','')))
    ask('TAILSCALE_ACCEPT_DNS','App Tailscale accept DNS?', get('TAILSCALE_ACCEPT_DNS', infra.get('APP_TAILSCALE_ACCEPT_DNS','false')), ['true','false'])
    ask_secret('TAILSCALE_AUTHKEY','Optional App Tailscale auth key')

sections={
 'frontend': frontend, 'db': db, 'security': security, 'mailersend': mailersend,
 'oauth': oauth, 'firebase': firebase, 'gcp-storage': gcp_storage, 'google-drive': google_drive, 'logging': logging, 'tailscale': tailscale,
}
if section == 'all':
    for name in ['frontend','db','security','mailersend','oauth','firebase','gcp-storage','google-drive','logging','tailscale']:
        sections[name]()
else:
    sections[section]()

out=[]; seen=set()
for line in app_lines:
    if line and not line.lstrip().startswith('#') and '=' in line:
        k=line.split('=',1)[0]
        if k in app:
            out.append(f"{k}={app[k]}"); seen.add(k)
        else: out.append(line)
    else: out.append(line)
for k in order:
    if k not in seen: out.append(f"{k}={app[k]}")
app_path.write_text('\n'.join(out).rstrip()+'\n')
app_path.chmod(0o600)
print(f"\nWrote {app_path}")
PY

apply_app() {
  echo "Applying App env to ${APP_INSTANCE_NAME}:~/app-dev/.env"
  local args=(--project "${PROJECT_ID}" --zone "${ZONE}")
  if [[ "${USE_IAP_FOR_APP}" == "true" ]]; then
    args+=(--tunnel-through-iap)
  fi
  gcloud compute scp "${APP_ENV_FILE}" "${APP_INSTANCE_NAME}:~/app-dev/$(basename "${APP_ENV_FILE}")" "${args[@]}"
  gcloud compute ssh "${APP_INSTANCE_NAME}" "${args[@]}" --command \
    "cd ~/app-dev && mv '$(basename "${APP_ENV_FILE}")' .env && chmod 600 .env && if systemctl list-unit-files | grep -q '^gjlearn-app.service'; then sudo systemctl restart gjlearn-app && sleep 3 && sudo systemctl is-active --quiet gjlearn-app && curl -fsS http://127.0.0.1:\${MANAGEMENT_PORT:-9090}/actuator/health; else echo 'App env updated; gjlearn-app.service not installed yet'; fi"
}

if [[ "${MODE}" == "--apply-app" ]]; then
  apply_app
else
  cat <<EOF
Local app env update complete: ${APP_ENV_FILE}
Apply this app env later with:
  scripts/gcp/03_env_render/03_configure-app-runtime-section.sh ${ENV_FILE} ${SECTION} --apply-app
EOF
fi
