# Harness

하네스 설계 문서는 `versions/` 같은 별도 묶음 폴더를 쓰지 않고, `v1`, `v2`처럼 버전 폴더를 바로 둔다. 각 버전 안에는 `{yyyy-mm-dd}` 날짜 폴더를 쌓아 같은 major version의 설계 변화를 추적한다.

## Registry

| Version | Date | Path | Status | Summary |
| --- | --- | --- | --- | --- |
| `v1` | 2026-06-28 | `harness/v1/2026-06-28/` | active | Codex one-shot task runner, GitHub issue/PR context capture, prompt/result hook monitoring, issue→branch→implementation→commit→PR workflow baseline |

## Versioning rules

- 폴더 구조는 `harness/v<major>/<yyyy-mm-dd>/` 형식이다.
- `v1`은 최초 운영 baseline이다.
- `v2`는 artifact DB, App Server, GitHub Action, worktree isolation처럼 실행 모델이 바뀔 때 만든다.
- 작은 문구 수정은 active date folder 안에서 고치되, workflow state machine이나 artifact contract가 바뀌면 같은 major의 새 날짜 폴더 또는 새 major version을 만든다.
- PR 본문에는 사용한 하네스 버전과 날짜를 함께 적는다. 예: `v1 (2026-06-28)`.

## Feature request quick start

시동어 규칙은 `harness/startup-words.md`를 본다. `/plan`은 issue draft/구현 계획만 만들고, `implement /goal ...` 또는 `implement --issue <번호>`는 하네스 구현 모드로 branch/task/PR handoff까지 진행한다.

```bash
# 최근 issue/PR/template 양식 확인
make harness-github-context

# GitHub issue에서 branch/task/pr-draft 준비
make harness-prepare-feature ISSUE=161

# 생성된 task packet을 Codex 실행 전 dry-run으로 검토
scripts/harness/run-task.sh --dry-run harness/tasks/issue-161-feature-task.md
```

기능 구현 PR은 최근 PR 관행을 따라 기본 섹션, 검증 command/result, reviewer note를 유지하고, 복잡한 흐름에만 Mermaid 시각화를 추가한다.
