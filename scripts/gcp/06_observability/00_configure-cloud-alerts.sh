#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:?usage: scripts/gcp/06_observability/00_configure-cloud-alerts.sh scripts/gcp/00_env/dev.env}"
if [[ ! -f "${ENV_FILE}" ]]; then
  echo "env file not found: ${ENV_FILE}" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "${ENV_FILE}"

: "${PROJECT_ID:?missing PROJECT_ID}"
: "${ENVIRONMENT:?missing ENVIRONMENT}"

gcloud config set project "${PROJECT_ID}" >/dev/null

gcloud services enable \
  logging.googleapis.com \
  monitoring.googleapis.com \
  --project="${PROJECT_ID}"

LOG_ID="${CLOUD_LOGGING_LOG_ID:-gjlearn-${ENVIRONMENT}-app}"
METRIC_NAME="${CLOUD_LOGGING_WARN_ERROR_METRIC_NAME:-gjlearn_${ENVIRONMENT}_app_warn_error_count}"
POLICY_DISPLAY_NAME="${CLOUD_LOGGING_WARN_ERROR_POLICY_NAME:-GJLearn ${ENVIRONMENT} app WARN/ERROR logs}"
NOTIFICATION_CHANNELS="${ALERT_NOTIFICATION_CHANNELS:-}"

LOG_FILTER="resource.type=\"gce_instance\" AND log_id(\"${LOG_ID}\")"

if gcloud logging metrics describe "${METRIC_NAME}" --project="${PROJECT_ID}" >/dev/null 2>&1; then
  gcloud logging metrics update "${METRIC_NAME}" \
    --project="${PROJECT_ID}" \
    --description="GJLearn ${ENVIRONMENT} app WARN/ERROR log entries from Cloud Ops Agent" \
    --log-filter="${LOG_FILTER}"
  echo "updated log-based metric: ${METRIC_NAME}"
else
  gcloud logging metrics create "${METRIC_NAME}" \
    --project="${PROJECT_ID}" \
    --description="GJLearn ${ENVIRONMENT} app WARN/ERROR log entries from Cloud Ops Agent" \
    --log-filter="${LOG_FILTER}"
  echo "created log-based metric: ${METRIC_NAME}"
fi

EXISTING_POLICY="$(gcloud monitoring policies list \
  --project="${PROJECT_ID}" \
  --filter="displayName=\"${POLICY_DISPLAY_NAME}\"" \
  --format='value(name)' \
  --limit=1)"

POLICY_FILE="$(mktemp)"
trap 'rm -f "${POLICY_FILE}"' EXIT

python - <<'PY' "${POLICY_FILE}" "${POLICY_DISPLAY_NAME}" "${METRIC_NAME}" "${NOTIFICATION_CHANNELS}" "${ENVIRONMENT}"
import json
import sys
from pathlib import Path

policy_file, display_name, metric_name, channels_csv, environment = sys.argv[1:]
channels = [c.strip() for c in channels_csv.split(',') if c.strip()]
policy = {
    "displayName": display_name,
    "combiner": "OR",
    "enabled": True,
    "documentation": {
        "content": (
            f"GJLearn {environment} App VM emitted at least one WARN/ERROR log entry. "
            "Inspect Cloud Logging and the VM journal/file logs before acknowledging."
        ),
        "mimeType": "text/markdown",
    },
    "conditions": [
        {
            "displayName": "WARN/ERROR log count > 0",
            "conditionThreshold": {
                "filter": f'metric.type="logging.googleapis.com/user/{metric_name}" AND resource.type="gce_instance"',
                "comparison": "COMPARISON_GT",
                "thresholdValue": 0,
                "duration": "0s",
                "trigger": {"count": 1},
                "aggregations": [
                    {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_DELTA",
                        "crossSeriesReducer": "REDUCE_SUM",
                    }
                ],
            },
        }
    ],
}
if channels:
    policy["notificationChannels"] = channels
Path(policy_file).write_text(json.dumps(policy, indent=2, ensure_ascii=False) + "\n")
PY

if [[ -n "${EXISTING_POLICY}" ]]; then
  gcloud monitoring policies update "${EXISTING_POLICY}" \
    --project="${PROJECT_ID}" \
    --policy-from-file="${POLICY_FILE}"
  echo "updated alert policy: ${EXISTING_POLICY}"
else
  gcloud monitoring policies create \
    --project="${PROJECT_ID}" \
    --policy-from-file="${POLICY_FILE}"
  echo "created alert policy: ${POLICY_DISPLAY_NAME}"
fi

cat <<EOF
Cloud Logging alert setup complete.
metric=${METRIC_NAME}
log_id=${LOG_ID}
filter=${LOG_FILTER}
notification_channels=${NOTIFICATION_CHANNELS:-<none configured>}
EOF
