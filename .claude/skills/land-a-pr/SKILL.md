---
name: land-a-pr
description: Use when implementation on a branch is done and the work needs to go out — verifies, writes the PR body in this repo's house format, opens the PR against dev, and merges after human approval. Triggers on "land-a-pr", "PR 올려줘", "이거 머지하자", or any request to open or land a pull request here.
---

# land-a-pr

끝난 브랜치를 검증하고 PR을 열어 병합까지 간다.

**절차는 [`harness/v2/2026-08-08/workflows/land-a-pr.md`](../../../harness/v2/2026-08-08/workflows/land-a-pr.md)에
있다. 그것을 읽고 그대로 따른다.**

시작할 때 알린다: "land-a-pr 워크플로로 PR을 준비합니다."

## 이 자리에서 놓치기 쉬운 것

- **증거 없이 완료를 주장하지 않는다.** `scripts/harness/verify.sh` 출력을 본 뒤에만
  통과라고 적는다. 「통과할 것이다」는 PR 본문에 안 들어간다
- 이 레포에는 PR 템플릿 파일이 없다. 양식은 최근 PR이 갖는다 —
  `gh pr view <최근 번호> --json body -q .body`로 확인하고 맞춘다
- **`리뷰어에게` 절이 제일 값나간다.** diff가 못 말하는 것, 즉 «왜 저 방식이
  아니었나»만 적는다
- **체크리스트를 안 한 것까지 체크하지 않는다.** 빈칸이 정보다
- 기준 브랜치는 `dev`. `main`으로 바로 열지 않는다
- `land` 게이트에 마이그레이션 유무를 실어 보낸다. **스키마가 바뀐 PR은 되돌리기가
  비싸다.** 판정은 [`gates-ledger.tsv`](../../../harness/gates-ledger.tsv)에 남긴다
- **자동 병합·자동 배포를 하지 않는다.** CI를 확인하고 사람이 누른다

`dev` → `main` 릴리스는 이 워크플로가 아니다.
