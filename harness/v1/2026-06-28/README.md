# Harness v1 — 2026-06-28

## 목적

`v1 (2026-06-28)`은 금정야학 백엔드의 모니터링, 동기화, 문서화 작업을 반복 가능한 `issue -> branch -> commit -> PR` 흐름으로 고정하는 첫 번째 하네스 baseline이다.

## 설계 판단

LLM Wiki의 Codex Harness Engineering 결론을 적용한다.

```text
Codex-style harness = task + turn + check + retry loop
Hermes = lifecycle owner / verifier / wiki bridge
Codex = repo-local executor / reviewer / bounded diff producer
```

v1은 App Server나 장기 DB를 도입하지 않는다. 우선 repo 안에서 다음을 버전 관리한다.

- task packet template
- prompt recipe
- result schema
- prompt/result monitor hook
- verification wrapper
- diff summary wrapper
- PR handoff contract

## 포함 문서

- `folder-structure.md` — 하네스 폴더 구조와 active/versioned 경계
- `workflow.md` — issue에서 PR까지 상태 전이
- `guide.md` — 지침 작성법, 가드레일, 팀 역할, 사용자 프롬프트 입력 수집 사용법
- `prompt-monitoring.md` — 프롬프트/결과/hook artifact 설계
- `prompts/issue-branch-commit-pr.md` — v1 active prompt recipe

## v1 done definition

- task file이 scope와 acceptance criteria를 명시한다.
- runner가 prompt를 렌더링하고 `monitor.jsonl`에 sha256/size를 남긴다.
- Codex 결과는 schema로 제한한다.
- runner 검증은 Codex self-report와 별도로 실행한다.
- PR에는 하네스 버전, issue 번호, 검증 명령, blocker를 적는다.

## v2 후보

- SQLite 기반 run ledger 추가
- per-issue git worktree isolation
- GitHub Actions read-only Codex review lane
- Codex App Server event/approval stream 직접 수집
- prompt recipe별 pass rate / hallucinated verification / unintended file modification metric 집계
