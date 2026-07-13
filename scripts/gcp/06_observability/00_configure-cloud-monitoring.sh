#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:?usage: scripts/gcp/06_observability/00_configure-cloud-monitoring.sh scripts/gcp/00_env/dev.env}"
if [[ ! -r "${ENV_FILE}" ]]; then
  echo "env file not found: ${ENV_FILE}" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "${ENV_FILE}"

: "${PROJECT_ID:?missing PROJECT_ID}"
: "${ENVIRONMENT:?missing ENVIRONMENT}"
: "${ZONE:?missing ZONE}"
: "${APP_INSTANCE_NAME:?missing APP_INSTANCE_NAME}"

gcloud config set project "${PROJECT_ID}" >/dev/null
gcloud services enable logging.googleapis.com monitoring.googleapis.com --project="${PROJECT_ID}"
gcloud logging buckets update _Default \
  --location=global \
  --retention-days=30 \
  --project="${PROJECT_ID}" >/dev/null

LOG_ID="${CLOUD_LOGGING_LOG_ID:-gjlearn-${ENVIRONMENT}-app}"
LOG_METRIC="${CLOUD_LOGGING_WARN_ERROR_METRIC_NAME:-gjlearn_${ENVIRONMENT}_app_warn_error_count}"
LOG_FILTER="resource.type=\"gce_instance\" AND log_id(\"${LOG_ID}\") AND (jsonPayload.message=~\" WARN \" OR jsonPayload.message=~\" ERROR \")"
NOTIFICATION_CHANNELS="${ALERT_NOTIFICATION_CHANNELS:-}"
APP_INSTANCE_ID="$(gcloud compute instances describe "${APP_INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --format='value(id)')"
DB_INSTANCE_ID=""
if [[ "${SKIP_DB_INSTANCE:-false}" != "true" ]]; then
  : "${DB_INSTANCE_NAME:?missing DB_INSTANCE_NAME}"
  DB_INSTANCE_ID="$(gcloud compute instances describe "${DB_INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --format='value(id)')"
fi

if gcloud logging metrics describe "${LOG_METRIC}" --project="${PROJECT_ID}" >/dev/null 2>&1; then
  gcloud logging metrics update "${LOG_METRIC}" --project="${PROJECT_ID}" \
    --description="GJLearn ${ENVIRONMENT} WARN/ERROR logs" --log-filter="${LOG_FILTER}"
else
  gcloud logging metrics create "${LOG_METRIC}" --project="${PROJECT_ID}" \
    --description="GJLearn ${ENVIRONMENT} WARN/ERROR logs" --log-filter="${LOG_FILTER}"
fi

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "${WORK_DIR}"' EXIT

python3 - "${WORK_DIR}" "${ENVIRONMENT}" "${LOG_METRIC}" "${LOG_ID}" "${NOTIFICATION_CHANNELS}" "${APP_INSTANCE_ID}" "${DB_INSTANCE_ID}" <<'PY'
import json
import sys
from pathlib import Path

out, env, log_metric, log_id, channels_csv, app_id, db_id = sys.argv[1:]
out = Path(out)
channels = [value.strip() for value in channels_csv.split(",") if value.strip()]
instance_ids = f"{app_id}|{db_id}" if db_id else app_id
instance_filter = f'resource.label.instance_id="{app_id}"'
if db_id:
    instance_filter += f' OR resource.label.instance_id="{db_id}"'

def promql(name, query, duration, summary):
    return {
        "displayName": name,
        "combiner": "OR",
        "enabled": True,
        "documentation": {"content": summary, "mimeType": "text/markdown"},
        "conditions": [{
            "displayName": name,
            "conditionPrometheusQueryLanguage": {
                "query": query,
                "duration": duration,
                "evaluationInterval": "60s",
                "disableMetricValidation": True,
            },
        }],
    }

def absent(name, instance_id, summary):
    return {
        "displayName": name,
        "combiner": "OR",
        "enabled": True,
        "documentation": {"content": summary, "mimeType": "text/markdown"},
        "conditions": [{
            "displayName": name,
            "conditionAbsent": {
                "filter": (
                    'metric.type="agent.googleapis.com/agent/uptime" '
                    'AND resource.type="gce_instance" '
                    f'AND resource.label.instance_id="{instance_id}"'
                ),
                "duration": "120s",
            },
        }],
    }

policies = [
    promql(
        f"GJLearn {env} API down",
        f'up{{env="{env}",service="api"}} == 0 or absent(up{{env="{env}",service="api"}})',
        "120s",
        f"GJLearn {env} API actuator has been unavailable for 2 minutes.",
    ),
    absent(f"GJLearn {env} App VM down", app_id, f"GJLearn {env} App VM Ops Agent stopped reporting."),
    absent(f"GJLearn {env} DB VM down", db_id, f"GJLearn {env} DB VM Ops Agent stopped reporting."),
    promql(
        f"GJLearn {env} PostgreSQL down",
        f'pg_up{{env="{env}"}} == 0 or absent(pg_up{{env="{env}"}})',
        "120s",
        f"GJLearn {env} PostgreSQL exporter reports the database unavailable.",
    ),
    promql(
        f"GJLearn {env} API 5xx ratio high",
        f'100 * sum(rate(http_server_requests_seconds_count{{env="{env}",status=~"5.."}}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{{env="{env}"}}[5m])), 0.001) > 5',
        "300s",
        f"GJLearn {env} API 5xx ratio exceeded 5% for 5 minutes.",
    ),
    promql(
        f"GJLearn {env} API p95 latency high",
        f'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{{env="{env}"}}[5m]))) > 1',
        "300s",
        f"GJLearn {env} API p95 latency exceeded 1 second for 5 minutes.",
    ),
    {
        "displayName": f"GJLearn {env} disk usage high",
        "combiner": "OR",
        "enabled": True,
        "documentation": {"content": f"GJLearn {env} VM disk usage exceeded 85% for 10 minutes.", "mimeType": "text/markdown"},
        "conditions": [{
            "displayName": "Disk used > 85%",
            "conditionThreshold": {
                "filter": (
                    'metric.type="agent.googleapis.com/disk/percent_used" '
                    'AND resource.type="gce_instance" AND metric.label.state="used" '
                    f'AND ({instance_filter})'
                ),
                "comparison": "COMPARISON_GT",
                "thresholdValue": 85,
                "duration": "600s",
                "trigger": {"count": 1},
                "aggregations": [{"alignmentPeriod": "60s", "perSeriesAligner": "ALIGN_MEAN"}],
            },
        }],
    },
    promql(
        f"GJLearn {env} DB connections high",
        f'sum(pg_stat_database_numbackends{{env="{env}"}}) / max(pg_settings_max_connections{{env="{env}"}}) > 0.8',
        "300s",
        f"GJLearn {env} PostgreSQL connection usage exceeded 80% for 5 minutes.",
    ),
    {
        "displayName": f"GJLearn {env} WARN/ERROR logs",
        "combiner": "OR",
        "enabled": True,
        "documentation": {"content": f"GJLearn {env} emitted a WARN or ERROR log.", "mimeType": "text/markdown"},
        "conditions": [{
            "displayName": "WARN/ERROR log count > 0",
            "conditionThreshold": {
                "filter": f'metric.type="logging.googleapis.com/user/{log_metric}" AND resource.type="gce_instance"',
                "comparison": "COMPARISON_GT",
                "thresholdValue": 0,
                "duration": "0s",
                "trigger": {"count": 1},
                "aggregations": [{"alignmentPeriod": "60s", "perSeriesAligner": "ALIGN_DELTA", "crossSeriesReducer": "REDUCE_SUM"}],
            },
        }],
    },
]

if not db_id:
    db_policy_names = {
        f"GJLearn {env} DB VM down",
        f"GJLearn {env} PostgreSQL down",
        f"GJLearn {env} DB connections high",
    }
    policies = [policy for policy in policies if policy["displayName"] not in db_policy_names]

for index, policy in enumerate(policies):
    if channels:
        policy["notificationChannels"] = channels
    (out / f"policy-{index}.json").write_text(json.dumps(policy, indent=2, ensure_ascii=False) + "\n")

def chart(title, query):
    return {
        "title": title,
        "xyChart": {
            "dataSets": [{
                "timeSeriesQuery": {"prometheusQuery": query},
                "plotType": "LINE",
            }],
            "timeshiftDuration": "0s",
        },
    }

dashboard = {
    "displayName": f"GJLearn {env} Operations",
    "mosaicLayout": {
        "columns": 12,
        "tiles": [
            {"xPos": 0, "yPos": 0, "width": 6, "height": 4, "widget": chart("API request rate", f'sum(rate(http_server_requests_seconds_count{{env="{env}"}}[5m]))')},
            {"xPos": 6, "yPos": 0, "width": 6, "height": 4, "widget": chart("API 5xx ratio", f'100 * sum(rate(http_server_requests_seconds_count{{env="{env}",status=~"5.."}}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{{env="{env}"}}[5m])), 0.001)')},
            {"xPos": 0, "yPos": 4, "width": 6, "height": 4, "widget": chart("API p95 latency", f'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{{env="{env}"}}[5m])))')},
            {"xPos": 6, "yPos": 4, "width": 6, "height": 4, "widget": chart("PostgreSQL connections", f'sum by (datname) (pg_stat_database_numbackends{{env="{env}"}})')},
            {"xPos": 0, "yPos": 8, "width": 4, "height": 4, "widget": chart("VM CPU", f'avg by (instance_id) (compute_googleapis_com:instance_cpu_utilization{{instance_id=~"{instance_ids}"}})')},
            {"xPos": 4, "yPos": 8, "width": 4, "height": 4, "widget": chart("VM memory used", f'avg by (instance_id) (agent_googleapis_com:memory_percent_used{{state="used",instance_id=~"{instance_ids}"}})')},
            {"xPos": 8, "yPos": 8, "width": 4, "height": 4, "widget": chart("VM disk used", f'max by (instance_id) (agent_googleapis_com:disk_percent_used{{state="used",instance_id=~"{instance_ids}"}})')},
            {"xPos": 0, "yPos": 12, "width": 4, "height": 4, "widget": chart("JVM memory", f'sum by (area) (jvm_memory_used_bytes{{env="{env}"}})')},
            {"xPos": 4, "yPos": 12, "width": 4, "height": 4, "widget": chart("JVM threads", f'jvm_threads_live_threads{{env="{env}"}}')},
            {"xPos": 8, "yPos": 12, "width": 4, "height": 4, "widget": chart("Hikari connections", f'sum by (state) (hikaricp_connections{{env="{env}"}})')},
            {"xPos": 0, "yPos": 16, "width": 6, "height": 4, "widget": chart("PostgreSQL cache hit", f'100 * sum(pg_stat_database_blks_hit{{env="{env}"}}) / clamp_min(sum(pg_stat_database_blks_hit{{env="{env}"}}) + pg_stat_database_blks_read{{env="{env}"}}), 1)')},
            {"xPos": 6, "yPos": 16, "width": 6, "height": 4, "widget": chart("PostgreSQL transactions", f'sum by (datname) (rate(pg_stat_database_xact_commit{{env="{env}"}}[5m]) + rate(pg_stat_database_xact_rollback{{env="{env}"}}[5m]))')},
            {"xPos": 0, "yPos": 20, "width": 12, "height": 4, "widget": {"title": "Recent WARN/ERROR logs", "logsPanel": {"filter": f'resource.type="gce_instance" AND log_id("{log_id}") AND (jsonPayload.message=~" WARN " OR jsonPayload.message=~" ERROR ")'}}},
        ],
    },
}
if not db_id:
    dashboard["mosaicLayout"]["tiles"] = [
        tile for tile in dashboard["mosaicLayout"]["tiles"]
        if not tile["widget"].get("title", "").startswith("PostgreSQL")
    ]
(out / "dashboard.json").write_text(json.dumps(dashboard, indent=2, ensure_ascii=False) + "\n")
PY

for policy_file in "${WORK_DIR}"/policy-*.json; do
  display_name="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["displayName"])' "${policy_file}")"
  existing="$(gcloud monitoring policies list --project="${PROJECT_ID}" --filter="displayName=\"${display_name}\"" --format='value(name)' --limit=1)"
  if [[ -n "${existing}" ]]; then
    gcloud monitoring policies update "${existing}" --project="${PROJECT_ID}" --policy-from-file="${policy_file}"
  else
    gcloud monitoring policies create --project="${PROJECT_ID}" --policy-from-file="${policy_file}"
  fi
done

if [[ "${SKIP_DB_INSTANCE:-false}" == "true" ]]; then
  for stale_display_name in \
    "GJLearn ${ENVIRONMENT} DB VM down" \
    "GJLearn ${ENVIRONMENT} PostgreSQL down" \
    "GJLearn ${ENVIRONMENT} DB connections high"; do
    while IFS= read -r stale_policy; do
      [[ -z "${stale_policy}" ]] && continue
      gcloud monitoring policies delete "${stale_policy}" --project="${PROJECT_ID}" --quiet
    done < <(gcloud monitoring policies list \
      --project="${PROJECT_ID}" \
      --filter="displayName=\"${stale_display_name}\"" \
      --format='value(name)')
  done
fi

LEGACY_POLICY_DISPLAY_NAME="${CLOUD_LOGGING_WARN_ERROR_POLICY_NAME:-GJLearn ${ENVIRONMENT} app WARN/ERROR logs}"
if [[ "${LEGACY_POLICY_DISPLAY_NAME}" != "GJLearn ${ENVIRONMENT} WARN/ERROR logs" ]]; then
  while IFS= read -r legacy_policy; do
    [[ -z "${legacy_policy}" ]] && continue
    gcloud monitoring policies delete "${legacy_policy}" --project="${PROJECT_ID}" --quiet
  done < <(gcloud monitoring policies list \
    --project="${PROJECT_ID}" \
    --filter="displayName=\"${LEGACY_POLICY_DISPLAY_NAME}\"" \
    --format='value(name)')
fi

DASHBOARD_NAME="GJLearn ${ENVIRONMENT} Operations"
EXISTING_DASHBOARD="$(gcloud monitoring dashboards list --project="${PROJECT_ID}" --filter="displayName=\"${DASHBOARD_NAME}\"" --format='value(name)' --limit=1)"
if [[ -n "${EXISTING_DASHBOARD}" ]]; then
  ETAG="$(gcloud monitoring dashboards describe "${EXISTING_DASHBOARD}" --project="${PROJECT_ID}" --format='value(etag)')"
  python3 - "${WORK_DIR}/dashboard.json" "${ETAG}" <<'PY'
import json
import sys
from pathlib import Path
path = Path(sys.argv[1])
dashboard = json.loads(path.read_text())
dashboard["etag"] = sys.argv[2]
path.write_text(json.dumps(dashboard, indent=2, ensure_ascii=False) + "\n")
PY
  gcloud monitoring dashboards update "${EXISTING_DASHBOARD}" --project="${PROJECT_ID}" --config-from-file="${WORK_DIR}/dashboard.json"
else
  gcloud monitoring dashboards create --project="${PROJECT_ID}" --config-from-file="${WORK_DIR}/dashboard.json"
fi

echo "Cloud Monitoring configured: environment=${ENVIRONMENT}, log_id=${LOG_ID}"
