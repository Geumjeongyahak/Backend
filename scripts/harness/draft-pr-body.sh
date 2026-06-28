#!/usr/bin/env bash
set -euo pipefail

ISSUE_NUMBER="${1:-}"
if [[ -z "${ISSUE_NUMBER}" || ! "${ISSUE_NUMBER}" =~ ^[0-9]+$ ]]; then
  echo "usage: $0 <issue-number>" >&2
  exit 2
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "gh command not found" >&2
  exit 127
fi

ISSUE_JSON="$(mktemp)"
trap 'rm -f "${ISSUE_JSON}"' EXIT
gh issue view "${ISSUE_NUMBER}" --json number,title,body,url > "${ISSUE_JSON}"

python3 - "${ISSUE_JSON}" <<'PY'
import json
import re
import sys
from pathlib import Path

issue = json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))
title = re.sub(r'^\[[^\]]+\]\s*', '', issue['title']).strip()

print(f"""## 개요

{title} 기능 요청을 기존 금정야학 백엔드 도메인 구조와 API 컨벤션에 맞춰 구현합니다.

Harness version: `v1-2026-06-28`

## 변경 유형

- [x] 새 기능 (feat)
- [ ] 버그 수정 (fix)
- [ ] 리팩토링 (refactor)
- [ ] 문서 수정 (docs)
- [ ] 테스트 (test)
- [ ] 기타 (chore)

## 변경 내용

- TODO: 구현한 도메인/API/DB/문서 변경을 bullet로 정리합니다.
- TODO: 권한, validation, transaction, 예외 처리 정책을 명시합니다.
- TODO: 기존 코드/PR convention을 따라 scope 밖 변경은 제외했다고 적습니다.

## 관련 이슈

Closes #{issue['number']}

## 스크린샷 (선택)

- 해당 없음 또는 관리자 화면/API 응답 증거를 첨부합니다.

## 체크리스트

- [ ] 코드 컨벤션을 준수했습니다
- [ ] 테스트 코드를 작성/수정했습니다
- [ ] 머지 전 `./gradlew test` 또는 필요한 focused test가 통과합니다
- [ ] 문서를 업데이트했습니다 (필요한 경우)

## 검증

- [ ] `git diff --check`
- [ ] `./gradlew test --tests '<focused-test>'`
- [ ] `scripts/harness/verify.sh`

## 리뷰어에게

- TODO: DB migration/API contract/security/운영 위험을 적습니다.
- TODO: API/DB/도메인 흐름이 복잡하면 최근 PR처럼 Mermaid를 추가합니다.
- TODO: 미검증 항목이나 blocker가 있으면 명확히 남깁니다.
""")
PY
