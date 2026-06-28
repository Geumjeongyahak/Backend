#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "usage: $0 <run-id> <event> [file] [status]" >&2
  exit 2
fi

RUN_ID="$1"
EVENT="$2"
FILE_PATH="${3:-}"
STATUS="${4:-ok}"

REPO_ROOT="$(git rev-parse --show-toplevel)"
ARTIFACT_DIR="${HARNESS_ARTIFACT_DIR:-${REPO_ROOT}/harness/runs/${RUN_ID}}"
MONITOR_FILE="${ARTIFACT_DIR}/monitor.jsonl"
mkdir -p "${ARTIFACT_DIR}"

python3 - "$RUN_ID" "$EVENT" "$FILE_PATH" "$STATUS" "$MONITOR_FILE" <<'PY'
import datetime as dt
import hashlib
import json
import os
import sys

run_id, event, file_path, status, monitor_file = sys.argv[1:]
record = {
    "timestamp": dt.datetime.now(dt.timezone.utc).isoformat(),
    "run_id": run_id,
    "event": event,
    "status": status,
    "file": file_path or None,
    "size_bytes": None,
    "sha256": None,
}

if file_path and os.path.exists(file_path) and os.path.isfile(file_path):
    record["size_bytes"] = os.path.getsize(file_path)
    h = hashlib.sha256()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    record["sha256"] = h.hexdigest()

with open(monitor_file, "a", encoding="utf-8") as f:
    f.write(json.dumps(record, ensure_ascii=False, sort_keys=True) + "\n")
PY
