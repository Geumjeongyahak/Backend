---
name: ideate-issue
description: Use when the user brings a feature idea, a complaint about how something works today, or asks to create/refine a GitHub issue — brainstorms the request into a concrete GitHub issue plus a spec, without touching code or creating branches. Triggers on "ideate", "이슈 만들자", "기능 추가하고 싶은데", or any vague feature request that is not yet issue-shaped.
---

# ideate-issue

무엇을 왜 할지를 사람과 대화로 정하고, GitHub issue 하나와 `spec.md` 하나를 남긴다.

**절차는 [`harness/v2/2026-08-08/workflows/ideate-issue.md`](../../../harness/v2/2026-08-08/workflows/ideate-issue.md)에
있다. 그것을 읽고 그대로 따른다.**

시작할 때 알린다: "ideate-issue 워크플로로 요구사항을 구체화합니다."

## 이 자리에서 놓치기 쉬운 것

- **코드를 고치지 않는다. 브랜치도 안 만든다.** 이슈를 만드는 것과 그 일을 지금
  시작하는 것은 다른 결정이다
- **한 번에 하나씩 묻는다.** 질문을 뭉치면 앞의 것만 답이 온다
- **대화 하나가 이슈 하나다.** 항목이 다섯 개면 다섯 번 돈다
- 끝에 `spec` 게이트가 있다. 사람의 승인 없이 `gh issue create`로 넘어가지 않는다
- 판정은 [`harness/gates-ledger.tsv`](../../../harness/gates-ledger.tsv)에 한 행으로 남긴다

전제(`gh` 인증 등)는 [`setup.md`](../../../harness/v2/2026-08-08/setup.md)에 있다.
