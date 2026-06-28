#!/usr/bin/env bash
set -euo pipefail

DRY_RUN=false
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
  shift
fi

TASK_FILE="${1:-}"
if [[ -z "${TASK_FILE}" ]]; then
  echo "usage: $0 [--dry-run] <task-file>" >&2
  exit 2
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"

if [[ ! -f "${TASK_FILE}" ]]; then
  echo "task file not found: ${TASK_FILE}" >&2
  exit 2
fi

RUN_ID="${HARNESS_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
HARNESS_VERSION="${HARNESS_VERSION:-v1-2026-06-28}"
ARTIFACT_DIR="${REPO_ROOT}/harness/runs/${RUN_ID}"
PROMPT_FILE="${ARTIFACT_DIR}/prompt.txt"
EVENTS_FILE="${ARTIFACT_DIR}/events.jsonl"
RESULT_FILE="${ARTIFACT_DIR}/result.json"
VERIFY_LOG="${ARTIFACT_DIR}/verify.log"
VERIFY_SUMMARY="${ARTIFACT_DIR}/verify-summary.txt"
DIFF_SUMMARY="${ARTIFACT_DIR}/diff-summary.md"
SCHEMA_FILE="${REPO_ROOT}/harness/schemas/task-result.schema.json"
VERSIONED_PROMPT_FILE="${REPO_ROOT}/harness/versions/${HARNESS_VERSION}/prompts/issue-branch-commit-pr.md"
BASE_PROMPT_FILE="${VERSIONED_PROMPT_FILE}"
if [[ ! -f "${BASE_PROMPT_FILE}" ]]; then
  BASE_PROMPT_FILE="${REPO_ROOT}/harness/prompts/issue-branch-commit-pr.md"
fi
HOOK="${REPO_ROOT}/scripts/harness/monitor-hook.sh"

mkdir -p "${ARTIFACT_DIR}"

{
  echo "# Harness Run Prompt"
  echo
  echo "Run ID: ${RUN_ID}"
  echo "Harness version: ${HARNESS_VERSION}"
  echo "Task file: ${TASK_FILE}"
  echo
  echo "## Base prompt"
  cat "${BASE_PROMPT_FILE}"
  echo
  echo "## Task packet"
  cat "${TASK_FILE}"
} > "${PROMPT_FILE}"

HARNESS_ARTIFACT_DIR="${ARTIFACT_DIR}" "${HOOK}" "${RUN_ID}" "prompt.rendered" "${PROMPT_FILE}" "ok"

if [[ "${DRY_RUN}" == "true" ]]; then
  echo "DRY RUN: prompt rendered at ${PROMPT_FILE}"
  echo "Artifacts: ${ARTIFACT_DIR}"
  exit 0
fi

if ! command -v codex >/dev/null 2>&1; then
  echo "codex command not found" >&2
  exit 127
fi

echo "==> Running Codex task turn"
set +e
codex exec \
  --json \
  --sandbox workspace-write \
  --ask-for-approval never \
  --output-schema "${SCHEMA_FILE}" \
  -o "${RESULT_FILE}" \
  "$(cat "${PROMPT_FILE}")" \
  > "${EVENTS_FILE}"
CODEX_EXIT=$?
set -e

HARNESS_ARTIFACT_DIR="${ARTIFACT_DIR}" "${HOOK}" "${RUN_ID}" "codex.finished" "${RESULT_FILE}" "exit-${CODEX_EXIT}"

if [[ ${CODEX_EXIT} -ne 0 ]]; then
  echo "Codex failed with exit code ${CODEX_EXIT}. See ${EVENTS_FILE}" >&2
  exit "${CODEX_EXIT}"
fi

echo "==> Running harness verification"
set +e
scripts/harness/verify.sh > "${VERIFY_LOG}" 2>&1
VERIFY_EXIT=$?
set -e

{
  if [[ ${VERIFY_EXIT} -eq 0 ]]; then
    echo "PASS"
  else
    echo "FAIL"
  fi
  echo
  echo "command: scripts/harness/verify.sh"
  echo "exit_code: ${VERIFY_EXIT}"
  echo
  echo "Last 120 lines:"
  tail -n 120 "${VERIFY_LOG}" || true
} > "${VERIFY_SUMMARY}"

HARNESS_ARTIFACT_DIR="${ARTIFACT_DIR}" "${HOOK}" "${RUN_ID}" "verify.finished" "${VERIFY_SUMMARY}" "exit-${VERIFY_EXIT}"

echo "==> Summarizing diff"
scripts/harness/summarize-diff.sh > "${DIFF_SUMMARY}"
HARNESS_ARTIFACT_DIR="${ARTIFACT_DIR}" "${HOOK}" "${RUN_ID}" "diff.summarized" "${DIFF_SUMMARY}" "ok"

if [[ ${VERIFY_EXIT} -ne 0 ]]; then
  echo "Verification failed. See ${VERIFY_SUMMARY}" >&2
  exit "${VERIFY_EXIT}"
fi

echo "Artifacts written to: ${ARTIFACT_DIR}"
