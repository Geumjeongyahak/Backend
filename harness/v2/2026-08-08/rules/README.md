# rules — 무엇을 언제 읽나

워크플로는 **어떤 순서로 하나**를 정하고, 여기는 **무엇을 확인하나**를 정한다.
2026-09 코드 점검에서 나온 결함을 기준으로 추렸다. 도구가 아니라 문서다 — 읽고
눈으로 확인한다.

| 하려는 일 | 읽는 것 | 워크플로의 자리 |
|---|---|---|
| `plan.md`를 쓴다 | [`writing-a-plan.md`](writing-a-plan.md) | `work-an-issue` 10 |
| 있는 코드를 고친다 | [`changing-code.md`](changing-code.md) | `work-an-issue` 20 |
| 테스트를 쓴다 | [`testing.md`](testing.md) | `work-an-issue` 30 |
| 리뷰에 보내기 전에 diff를 훑는다 | [`diff-signals.md`](diff-signals.md) | `work-an-issue` 30과 40 사이 |
| codex 전에 규칙 전체로 1차 검증한다 | 위 넷 + `CLAUDE.md` | `work-an-issue` 35 |

실수를 알아챘으면 [`mistakes-ledger.md`](../../../mistakes-ledger.md)에 한 행을 적는다.
**같은 갈래가 두 번째 나오면 위 규칙 중 맞는 곳에 올린다.**

codex에 넘기는 리뷰 지침은 [`../codex-review-prompt.md`](../codex-review-prompt.md)에
따로 있다. 한 파일로 넘겨야 해서 나누지 않았다.

## 안 들인 것

| 무엇 | 왜 |
|---|---|
| 린터·구조 검사 | 이 레포는 검사를 `verify.sh` 하나로 둔다. 규칙을 기계로 강제하지 않는다 |
| 구현 먼저, 테스트 나중 | 이 레포는 테스트를 먼저 쓴다 |
| 단계마다 사람이 diff 확인 | 사람이 보는 자리는 게이트 셋이다 |
| 직군(tester 등) | v2에는 직군이 없다 |

옆 레포(`heymoa-ai/harness/v005-2026-08-19/rules`)에서 모양을 가져왔지만 내용은
이 레포의 결함에서 다시 썼다. 그쪽 규칙을 그대로 옮겨 쓰지 않는다.
