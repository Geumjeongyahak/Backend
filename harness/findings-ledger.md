# Findings Ledger

codex 리뷰 지적 한 건이 한 행이다. **반려한 것도 적는다** — 반려한 지적이 나중에
사고로 돌아오는지가 이 원장을 두는 이유의 절반이다.

기록하는 자리와 읽는 법은 [observability.md](v2/2026-08-08/observability.md#harnessfindings-ledgermd)에
있다. 리뷰 절차는 [codex-review.md](v2/2026-08-08/codex-review.md)다.

- `심각도` — `P1` 병합을 막는다 · `P2` 고쳐야 한다 · `P3` 알아두면 좋다
- `위험도` — `blast` 넓게 번진다 · `blocking` 막힌다 · `local` 그 자리뿐 · `speculative` 확인 안 됨
- `판단` — `수용` 또는 `반려`. **`반려`면 `사유`가 필수다**

| 날짜 | 이슈 | 심각도 | 위험도 | 지적 | 판단 | 사유 |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-28 | #225 | P2 | blast | 전역 예외 처리기가 락과 무관한 질의·트랜잭션 시간 초과까지 409 BIZ005 로 변환한다 (`GlobalExceptionHandler`) | 수용 | 락 획득 실패(`PessimisticLockingFailureException`)만 409 로 바꾸고 시간 초과는 5xx 로 남겼다 |
| 2026-09-28 | #225 | P3 | local | API 문서가 영수증 첨부까지 나중 요청이 409 를 받는다고 적었다. 실제로는 둘 다 201 이고 한 건만 저장된다 (`docs/api/PurchaseRequests.md`) | 수용 | 상태 전이·삭제와 영수증 첨부를 나눠 적었다 |
