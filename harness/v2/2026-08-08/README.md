# harness v2 (2026-08-08) — 워크플로 계약

v1은 task packet과 prompt recipe를 문서로 고정했다. 실행은 `run-task.sh`가 프롬프트를
렌더링하는 데까지였고, 순서·게이트·기록은 사람 머릿속에 있었다.

v2는 **순서를 문서로 고정하고 실행은 superpowers skill에 맡긴다.** 러너를 만들지 않는다.

## 러너를 안 만든 이유

옆 레포(`heymoa-ai/harness/v004-2026-08-01`)에 셸 2,560줄짜리 러너가 있다. 상태 파일,
게이트 판정, pane 배치, worktree 격리, 원장 집계가 전부 들어 있다. 그것을 여기로
옮기는 것도 검토했고 안 옮겼다.

- 러너가 강제하는 것 중 이 레포에 없어서 아픈 것은 **적대적 리뷰 한 자리**다. 나머지
  게이트는 사람이 대화창에 앉아 있으면 눈으로 본다.
- 러너는 자기가 도는 환경을 요구한다. v004는 tmux/wezterm pane, uv 툴체인, Linear API에
  묶여 있다. 여기는 셋 다 없다.
- **문서는 낡으면 읽히지 않고 끝나지만, 러너는 낡으면 막는다.** 유지비를 낼 사람이
  정해지기 전에는 막는 쪽을 안 세운다.

## 옆 레포에서 안 가져온 것

흐름의 모양은 v004에서 가져왔지만 **밑바탕이 다르다.** 그대로 옮기면 안 되는 자리를
적어 둔다 — 나중에 v004를 참고할 때 여기를 먼저 본다.

| | heymoa-ai v004 | 여기 v2 |
|---|---|---|
| 이슈 원본 | Linear (`scripts.linear`, `APP-327` 같은 키) | **GitHub issue** (`gh`, 숫자 번호) |
| 이슈 폴더 | `docs/issues/APP-N-<slug>/` | `docs/issues/<번호>-<slug>/` |
| 검사 | `ruff` · `ruff format` · `mypy` · `lint-imports` · `pytest` (uv) | `scripts/harness/verify.sh` 하나 (**Gradle**) |
| 코드베이스 | Python · FastAPI · LangGraph | **Java 21 · Spring Boot 3.5 · JPA** |
| 테스트 층 | `tests/unit` · `tests/integration` | `e2e/` · `unit/` · `domain/` **셋** |
| 기준 브랜치 | 이슈의 부모가 정한다 | 언제나 `dev` |
| 화면 | tmux/wezterm pane 보드 | 없다. 기록은 파일로 |

**리뷰 프롬프트는 특히 옮겨 쓰면 안 된다.** v004의 것은 레이어 방향과
`toolCredentials` 유출을 보라고 적혀 있고, 여기서는 그런 것이 없다. 이 레포용은
[`codex-review-prompt.md`](codex-review-prompt.md)에 새로 썼다.

그래서 v2가 가진 것은 계약이지 기계가 아니다. **이 계약은 스스로를 강제하지 못한다.**
건너뛰어도 아무것도 안 막는다. 강제 대신 두 개의 원장에 흔적을 남기게 했고, 흔적이
비어 있으면 안 지킨 것으로 읽는다. 그게 이 설계가 낼 수 있는 최대치다.

## 어디에 무엇이 있나

```
.claude/skills/<이름>/SKILL.md  진입점. 얇다. 아래 문서를 가리킨다

harness/
  findings-ledger.md            codex 지적 한 건이 한 행
  gates-ledger.tsv              게이트에서 무엇을 묻고 뭐라 답했나
  v2/2026-08-08/
    README.md                   이 문서
    setup.md                    전제. gh · codex · verify.sh
    codex-review-prompt.md      codex 에 같이 넘기는 리뷰 지침
    workflows/
      ideate-issue.md           무엇을 왜 할지 정한다 → GitHub issue + spec.md
      work-an-issue.md          이슈 하나를 설계→구현→테스트→리뷰로 끌고 간다
      land-a-pr.md              검증하고 PR을 열고 병합까지
    codex-review.md             다른 눈. codex CLI 리뷰 프로토콜
    observability.md            무엇을 어디에 남기나. 못 재는 것은 무엇인가

docs/issues/<번호>-<slug>/      이슈 하나가 폴더 하나 (spec.md · plan.md · review.md)
scripts/harness/verify.sh       검사. 게이트가 이것 하나를 부른다
```

`docs/issue/`(단수)는 v2 이전에 쓰던 자리다. 건드리지 않는다. 새 이슈 폴더는
`docs/issues/`(복수)에 쌓는다.

## 워크플로는 셋이다

사람이 독립적으로 시작할 수 있고, 끝나면 산출물이 남는 것만 워크플로다.

| 워크플로 | 무엇을 정하나 | 산출물 |
|---|---|---|
| [`ideate-issue`](workflows/ideate-issue.md) | 무엇을 왜 할지 | GitHub issue · `docs/issues/<번호>-<slug>/spec.md` |
| [`work-an-issue`](workflows/work-an-issue.md) | 어떻게 만들지 | branch · `plan.md` · 코드 · 테스트 · `review.md` |
| [`land-a-pr`](workflows/land-a-pr.md) | 내보내도 되는지 | PR · 병합 · 브랜치 정리 |

셋은 순서대로 이어지지만 각각 따로 시작할 수 있다. 이슈가 이미 충분히 구체적이면
`ideate-issue`를 건너뛰고 `work-an-issue`부터 시작한다. 다만 그때는 **`plan` 게이트에
"이력 없음"이 찍힌다** — 무엇을 왜 하는지 사람이 판단한 기록이 없다는 뜻이고, 그 상태로
승인하는 것은 승인하는 사람이 진다.

## 시동어

v1의 `/plan`·`implement /goal`을 대체한다. 시동어는 워크플로 이름 그대로다.

```text
ideate 결제 신청 반려 사유 알림 기능
work-an-issue 219
land-a-pr 219
```

`--issue` 같은 플래그를 안 둔다. v1은 `/plan`과 `/plan --issue`가 다른 절차였고,
사용자가 어느 쪽을 불렀는지를 나중에 대화 로그로 되짚어야 했다. 워크플로 이름 하나에
인자 하나면 무엇이 돌았는지가 원장에 그대로 남는다.

## 붙는 방식

**플러그인 성격이다.** 설치할 것이 없고, 진입점만 `.claude/skills/`에 둔다. 대화
세션이 그 이름을 보고 이 폴더의 문서를 읽는다.

```
.claude/skills/<워크플로 이름>/SKILL.md   ──▶  harness/v2/2026-08-08/workflows/<이름>.md
```

진입점은 얇다. **절차는 전부 이 폴더가 갖는다.** 진입점에 절차를 복사하면 두 벌이
되고, 갱신될 때 한쪽만 낡는다.

바깥 플러그인(superpowers 등)에 절차를 위임하지 않는다. 위임하면 그것이 안 걸린
사람에게는 단계 이름만 남고 절차가 없다. 다만 이름은 겹치게 뒀다 — 세션에 그런
skill이 걸려 있으면 그것이 먼저 잡히고, 여기 적힌 게이트와 산출물 위치가 그 위에
얹힌다.

| 단계 | 하는 일 | 이 문서가 고정하는 것 |
|---|---|---|
| 구체화 | 대화로 요구사항을 좁힌다 | 산출물을 GitHub issue 양식에 맞춘다 |
| 설계 | `plan.md`를 쓴다 | `plan` 게이트. 맡은 것과 계획한 것을 나란히 놓는다 |
| 구현 | 테스트 먼저, 작은 커밋 | 커밋 단위를 Conventional Commits로 |
| 검증 | 검사를 돌린다 | `verify.sh` 하나로 고정 |
| 리뷰 | 다른 눈에 보낸다 | codex CLI로 못박는다 |
| 마무리 | PR을 열고 병합한다 | PR 양식과 원장 기록 |

## 게이트

게이트는 켜고 끄는 플래그가 아니라 **누가 판정하나**다. v2에는 판정자가 사람뿐이다
(무인 실행이 없으므로 `approver` 직군도 없다). 게이트는 셋이고 전부
[`gates-ledger.tsv`](../../gates-ledger.tsv)에 한 행씩 남는다.

| 게이트 | 어디 | 묻는 것 |
|---|---|---|
| `spec` | `ideate-issue` 끝 | 이 이슈로 작업을 시작해도 되나 |
| `plan` | `work-an-issue` 설계 뒤 | 이 계획으로 구현해도 되나 |
| `land` | `land-a-pr` 병합 전 | 내보내도 되나 |

**`막힘`은 예약어다.** 근거를 못 대면 통과가 아니라 막힘이다. 사람이 "괜찮아 보이는데"로
넘기는 것과 근거를 적고 통과시키는 것은 원장에서 구별돼야 한다.

## 검사

게이트가 부르는 검사는 [`scripts/harness/verify.sh`](../../../scripts/harness/verify.sh)
하나다. 지금 다섯을 돈다.

```
git diff --check          공백·충돌 마커
bash -n scripts/**/*.sh   셸 문법
observability-config-test.sh  관측 설정 계약
harness/schemas/*.json    스키마 파싱
./gradlew test            전체 테스트
```

**직군마다 다시 돌리지 않는다.** 전체 테스트가 분 단위라 구현·테스트·리뷰에서 세 번
돌면 그만큼이 그대로 대기 시간이다. 한 워크플로 안에서 코드가 안 바뀌었으면 앞의
결과를 그대로 쓴다. v004는 이것을 트리 해시로 캐시했는데, 러너가 없는 v2에서는
**사람이 "코드가 안 바뀌었다"를 판단한다.** 애매하면 다시 돈다.

## v3 후보

- 게이트를 강제하는 자리. 지금은 건너뛰어도 아무것도 안 막는다
- 토큰·시간 측정. 러너가 없어서 못 잰다 ([observability.md](observability.md) 참조)
- 무인 실행과 worktree 격리
- `codex review` 호출을 세는 자리. 지금은 안 부르고 병합해도 흔적이 안 남는다
