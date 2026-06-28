# Harness

하네스 설계 문서는 `versions/` 같은 별도 묶음 폴더를 쓰지 않고, `v1`, `v2`처럼 버전 폴더를 바로 둔다. 각 버전 안에는 `{yyyy-mm-dd}` 날짜 폴더를 쌓아 같은 major version의 설계 변화를 추적한다.

## Registry

| Version | Date | Path | Status | Summary |
| --- | --- | --- | --- | --- |
| `v1` | 2026-06-28 | `harness/v1/2026-06-28/` | active | Codex one-shot task runner, prompt/result hook monitoring, issue→branch→commit→PR workflow baseline |

## Versioning rules

- 폴더 구조는 `harness/v<major>/<yyyy-mm-dd>/` 형식이다.
- `v1`은 최초 운영 baseline이다.
- `v2`는 artifact DB, App Server, GitHub Action, worktree isolation처럼 실행 모델이 바뀔 때 만든다.
- 작은 문구 수정은 active date folder 안에서 고치되, workflow state machine이나 artifact contract가 바뀌면 같은 major의 새 날짜 폴더 또는 새 major version을 만든다.
- PR 본문에는 사용한 하네스 버전과 날짜를 함께 적는다. 예: `v1 (2026-06-28)`.
