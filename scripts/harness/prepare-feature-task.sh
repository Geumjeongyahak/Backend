#!/usr/bin/env bash
set -euo pipefail

DRY_RUN=false
ISSUE_NUMBER=""
RUN_ID="${HARNESS_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"

usage() {
  cat >&2 <<'EOF'
usage: scripts/harness/prepare-feature-task.sh [--dry-run] --issue <number>

Creates a feature implementation task packet from an existing GitHub issue,
collects recent issue/PR style context, and prepares the conventional feature
branch name. By default it switches to the branch; --dry-run only writes artifacts.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --issue)
      ISSUE_NUMBER="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
done

if [[ -z "${ISSUE_NUMBER}" ]]; then
  usage
  exit 2
fi

if ! [[ "${ISSUE_NUMBER}" =~ ^[0-9]+$ ]]; then
  echo "issue number must be numeric: ${ISSUE_NUMBER}" >&2
  exit 2
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "gh command not found" >&2
  exit 127
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"

if [[ "${HARNESS_ALLOW_DIRTY:-false}" != "true" ]] && [[ -n "$(git status --porcelain)" ]]; then
  echo "working tree is dirty; commit/stash first or set HARNESS_ALLOW_DIRTY=true" >&2
  git status --short >&2
  exit 3
fi

ARTIFACT_DIR="${REPO_ROOT}/harness/runs/${RUN_ID}"
TASK_DIR="${REPO_ROOT}/harness/tasks"
mkdir -p "${ARTIFACT_DIR}" "${TASK_DIR}"

ISSUE_JSON="${ARTIFACT_DIR}/issue-${ISSUE_NUMBER}.json"
ISSUE_BODY="${ARTIFACT_DIR}/issue-${ISSUE_NUMBER}.md"
CONTEXT_FILE="${ARTIFACT_DIR}/github-context.md"
BRANCH_FILE="${ARTIFACT_DIR}/branch-name.txt"
TASK_FILE="${TASK_DIR}/issue-${ISSUE_NUMBER}-feature-task.md"
PR_DRAFT="${ARTIFACT_DIR}/pr-draft.md"

gh issue view "${ISSUE_NUMBER}" --json number,title,body,labels,state,url > "${ISSUE_JSON}"
python3 - "${ISSUE_JSON}" "${ISSUE_BODY}" "${BRANCH_FILE}" <<'PY'
import json
import re
import sys
from pathlib import Path

issue_path, body_path, branch_path = map(Path, sys.argv[1:])
issue = json.loads(issue_path.read_text(encoding='utf-8'))
title = issue['title']
body_path.write_text(issue.get('body') or '', encoding='utf-8')

clean = re.sub(r'^\[[^\]]+\]\s*', '', title).lower()
clean = re.sub(r'[^0-9a-z가-힣]+', '-', clean).strip('-')
slug = clean[:40].strip('-') or 'feature'
branch_path.write_text(f"feature/{issue['number']}-{slug}\n", encoding='utf-8')
PY

BRANCH_NAME="$(tr -d '\n' < "${BRANCH_FILE}")"
scripts/harness/collect-github-context.sh > "${CONTEXT_FILE}"
scripts/harness/draft-pr-body.sh "${ISSUE_NUMBER}" > "${PR_DRAFT}"

python3 - \
  "${TASK_FILE}" \
  "${ISSUE_NUMBER}" \
  "${RUN_ID}" \
  "${BRANCH_NAME}" \
  "${ISSUE_JSON#${REPO_ROOT}/}" \
  "${ISSUE_BODY#${REPO_ROOT}/}" <<'PY'
import sys
from pathlib import Path

task_file = Path(sys.argv[1])
issue_number = sys.argv[2]
run_id = sys.argv[3]
branch_name = sys.argv[4]
issue_json = sys.argv[5]
issue_body = sys.argv[6]

content = f"""# Task: GitHub issue #{issue_number} 기능 구현

## Goal

GitHub issue #{issue_number}의 기능 요청을 기존 코드/문서/테스트 컨벤션에 맞춰 구현하고, 작은 논리 commit과 PR handoff까지 준비한다.

## Issue

- GitHub issue: #{issue_number}
- Issue snapshot: `{issue_json}`
- Issue body: `{issue_body}`
- Suggested branch: `{branch_name}`

## Read first

- `AGENTS.md`
- `CLAUDE.md`
- `HARNESS.md`
- `harness/startup-words.md`
- `harness/runs/{run_id}/github-context.md`
- `harness/runs/{run_id}/issue-{issue_number}.md`
- `docs/api/` 중 issue가 언급한 API 문서
- 구현 대상 도메인의 `entity/`, `repository/`, `service/`, `v1/controller/`, `v1/dto/`, 관련 테스트

## Discover before modify

- `search_files`/ripgrep으로 issue 키워드, 유사 API, DTO, service, exception, test를 찾는다.
- 기존 merged/open PR 본문에서 기본 섹션과 체크리스트 스타일을 확인한다.
- 변경 전 `git status --short --branch`, `git diff --stat`를 기록한다.

## Modify

Allowed scope is the minimum set required by the issue:

- `src/main/java/geumjeongyahak/domain/**`
- `src/main/resources/**` only if schema/config change is required
- `src/test/**`
- `docs/api/**`, `docs/**` only when contract/docs change
- `harness/runs/{run_id}/pr-draft.md` for PR handoff notes

## Constraints

- 기존 도메인 패키지 구조와 API 응답 형식을 따른다: `ApiResponse.success/error`, domain event boundary, BaseEntity, DTO request/response 분리.
- 새 public API나 DB contract 변경은 docs와 테스트를 함께 갱신한다.
- secrets, `.env`, credential 파일은 읽거나 수정하지 않는다.
- destructive command, deploy, force push, auto merge는 실행하지 않는다.
- 커밋은 사용자 또는 runner가 명시적으로 허용한 경우에만 수행한다.

## Acceptance criteria

- issue의 체크박스 요구사항이 구현/미구현/범위 밖으로 추적된다.
- 기존 유사 코드의 naming, exception, validation, transaction, security convention을 따른다.
- API/DB/도메인 흐름 변화가 PR 본문에 설명된다. 복잡한 흐름은 최근 PR처럼 Mermaid를 추가한다.
- 테스트 또는 명시적 blocker가 있다. Codex/Hermes self-report만으로 완료 처리하지 않는다.

## Verification

Prefer focused tests first, then full gate:

- `git diff --check`
- 변경 도메인 관련 `./gradlew test --tests '<focused-test>'`
- `scripts/harness/verify.sh`
- `scripts/harness/summarize-diff.sh`

## PR handoff

Use `harness/runs/{run_id}/pr-draft.md` as the starting body. Fill in:

- 개요
- 변경 유형
- 변경 내용
- 관련 이슈: `Closes #{issue_number}`
- 스크린샷 (선택)
- 체크리스트
- 검증: command + result
- 리뷰어에게: risk/blocker/운영 영향

## Output

Return JSON matching `harness/schemas/task-result.schema.json`.
"""
task_file.write_text(content, encoding='utf-8')
PY

"${REPO_ROOT}/scripts/harness/monitor-hook.sh" "${RUN_ID}" "github.context" "${CONTEXT_FILE}" "ok"
"${REPO_ROOT}/scripts/harness/monitor-hook.sh" "${RUN_ID}" "issue.snapshot" "${ISSUE_JSON}" "ok"
"${REPO_ROOT}/scripts/harness/monitor-hook.sh" "${RUN_ID}" "task.prepared" "${TASK_FILE}" "ok"
"${REPO_ROOT}/scripts/harness/monitor-hook.sh" "${RUN_ID}" "pr.drafted" "${PR_DRAFT}" "ok"

if [[ "${DRY_RUN}" == "true" ]]; then
  echo "DRY RUN: prepared task packet ${TASK_FILE}"
  echo "Suggested branch: ${BRANCH_NAME}"
  echo "Artifacts: ${ARTIFACT_DIR}"
  exit 0
fi

if git show-ref --verify --quiet "refs/heads/${BRANCH_NAME}"; then
  git switch "${BRANCH_NAME}"
else
  git switch -c "${BRANCH_NAME}"
fi

echo "Prepared task packet: ${TASK_FILE}"
echo "Branch: ${BRANCH_NAME}"
echo "Artifacts: ${ARTIFACT_DIR}"
