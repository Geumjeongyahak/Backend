# Harness Versions

하네스 설계 문서는 버전과 날짜를 함께 쌓는다. 새 구조를 크게 바꿀 때 기존 문서를 덮어쓰지 말고 `vN-yyyy-mm-dd/`를 추가한다.

## Registry

| Version | Date | Status | Summary |
| --- | --- | --- | --- |
| `v1-2026-06-28` | 2026-06-28 | active | Codex one-shot task runner, prompt/result hook monitoring, issue→branch→commit→PR workflow baseline |

## Versioning rules

- 폴더명은 `v<major>-<yyyy-mm-dd>` 형식이다.
- `v1`은 최초 운영 baseline이다.
- `v2`는 artifact DB, App Server, GitHub Action, worktree isolation처럼 실행 모델이 바뀔 때 만든다.
- 작은 문구 수정은 active version 안에서 고치되, workflow state machine이나 artifact contract가 바뀌면 새 버전을 만든다.
- PR 본문에는 사용한 하네스 버전을 적는다.
