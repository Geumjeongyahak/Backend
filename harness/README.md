# Harness

하네스 설계 문서는 `versions/` 같은 별도 묶음 폴더를 쓰지 않고, `v1`, `v2`처럼 버전 폴더를 바로 둔다. 각 버전 안에는 `{yyyy-mm-dd}` 날짜 폴더를 쌓아 같은 major version의 설계 변화를 추적한다.

## Registry

| Version | Date | Path | Status | Summary |
| --- | --- | --- | --- | --- |
| `v2` | 2026-08-08 | `harness/v2/2026-08-08/` | active | 워크플로 계약 셋(ideate-issue · work-an-issue · land-a-pr), 게이트 셋(spec · plan · land), codex CLI 적대적 리뷰, 원장 둘. 러너 없음 |
| `v1` | 2026-06-28 | `harness/v1/2026-06-28/` | superseded | Codex one-shot task runner, GitHub issue/PR context capture, prompt/result hook monitoring, issue→branch→implementation→commit→PR workflow baseline |

## Versioning rules

- 폴더 구조는 `harness/v<major>/<yyyy-mm-dd>/` 형식이다.
- `v1`은 최초 운영 baseline이다.
- 작은 문구 수정은 active date folder 안에서 고치되, workflow 순서나 게이트 계약이 바뀌면 같은 major의 새 날짜 폴더 또는 새 major version을 만든다.
- PR 본문에는 사용한 하네스 버전과 날짜를 함께 적는다. 예: `v2 (2026-08-08)`.

## Quick start

전제와 워크플로는 [`v2/2026-08-08/README.md`](v2/2026-08-08/README.md)를 본다.
진입점은 `.claude/skills/`에 있고 설치할 것은 없다.

```text
ideate 결제 신청 반려 사유 알림 기능    # 무엇을 왜 할지 정한다 → issue + spec.md
work-an-issue 219                     # 설계 → 구현 → 테스트 → codex 리뷰
land-a-pr 219                         # 검증 → PR → 병합
```

```bash
scripts/harness/verify.sh    # 검사. 게이트가 부르는 것은 이것 하나다
```

## 원장

이슈 폴더(`docs/issues/<번호>-<slug>/`)는 이슈 하나의 이야기고, 원장은 이슈를
가로지르는 이야기다. 재발과 판단 습관은 한 이슈 안에서는 안 보인다.

- [`findings-ledger.md`](findings-ledger.md) — codex 지적 한 건이 한 행
- [`gates-ledger.tsv`](gates-ledger.tsv) — 게이트에서 무엇을 묻고 뭐라 답했나

무엇이 남고 무엇이 안 남는지는 [`observability.md`](v2/2026-08-08/observability.md)에
있다. **못 재는 것은 못 잰다고 적혀 있다.**

## v1 자산

`v1`은 superseded지만 `scripts/harness/`의 스크립트와 `harness/prompts/`,
`harness/tasks/`는 그대로 쓴다. `verify.sh`는 v2의 유일한 검사이고, 나머지
(`collect-github-context.sh`, `prepare-feature-task.sh`, `run-task.sh`,
`summarize-diff.sh`)는 `make harness-*` 타깃으로 남아 있다. v2가 그것들을 요구하지는
않는다.

`startup-words.md`의 `/plan`·`implement /goal`은 v2의 워크플로 이름으로 대체됐다.
