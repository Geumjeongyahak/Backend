---
name: work-an-issue
description: Use when starting implementation work on an existing GitHub issue in this repo — drives one issue from branch through plan, TDD implementation, tests, and adversarial codex review. Triggers on "work-an-issue", "이슈 N번 작업하자", "N번 구현해줘", or any request to implement a numbered issue.
---

# work-an-issue

이슈 하나를 받아 브랜치·설계·구현·테스트·리뷰까지 끌고 간다.

**절차는 [`harness/v2/2026-08-08/workflows/work-an-issue.md`](../../../harness/v2/2026-08-08/workflows/work-an-issue.md)에
있다. 그것을 읽고 그대로 따른다.**

시작할 때 알린다: "work-an-issue 워크플로로 #N을 구현합니다."

## 이 자리에서 놓치기 쉬운 것

- **시작 전에 `scripts/harness/verify.sh` 기준선을 잡는다.** 원래 깨져 있었는지를
  나중에는 못 가린다
- `docs/issues/<번호>-<slug>/` 폴더가 없으면 멈춘다. **무엇을 할지 고르는 것은 사람의 일이다**
- 설계 뒤 `plan` 게이트가 **사람이 봐야 하는 유일한 자리**다. 맡은 것(이슈 제목)과
  계획한 것(`plan.md` 첫 줄)을 나란히 놓고 묻는다. 되돌리기 예산 2회
- 테스트를 먼저 쓰고 실패를 본다. 날짜는 미래 기준으로 만든다
- 리뷰는 [`codex-review.md`](../../../harness/v2/2026-08-08/codex-review.md) 프로토콜.
  `P1`·`P2`가 남으면 실패로 끝내지 말고 **구현으로 되돌아간다**
- 게이트는 [`gates-ledger.tsv`](../../../harness/gates-ledger.tsv), 지적은
  [`findings-ledger.md`](../../../harness/findings-ledger.md)

기준 브랜치는 `dev`다. 커밋은 Conventional Commits.
