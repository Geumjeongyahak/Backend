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
| 2026-09-29 | #231 | P2 | blast | 새 Drive 파일 정보 조회가 응답 시간 제한 없이 DB 트랜잭션 안에서 불려, Drive가 멈추면 DB 연결이 묶인다 (`DriveFileService`) | 수용 (방법은 다름) | 트랜잭션 밖으로 옮기지 않고 조회에 설정 가능한 응답 제한(`app.file.drive.metadata-timeout`, 기본 10초)을 뒀다. 등록은 드물고, #230 메일과 같은 판단이다 |
| 2026-09-29 | #231 | P2 | local | Drive가 준 파일명 · 형식이 컬럼 길이를 넘으면 DB 오류로 500 이 된다. 전에는 요청 DTO의 `@Size`가 막았다 (`DriveFileService`) | 수용 | 저장 전에 길이를 보고 400 으로 거절한다. 확장자(20자)가 넘으면 기존처럼 `drive` 로 둔다 |
| 2026-09-29 | #231 | P2 | blocking | 응답 제한이 OAuth 토큰 갱신(`refreshIfExpired`)은 덮지 못해, 토큰 서버가 느리면 트랜잭션이 10초를 넘어 연결을 잡는다 (`GoogleDriveStorageService`) | 수용 (1회차 지적의 방법을 바꿈) | 등록 메서드를 `NOT_SUPPORTED`로 트랜잭션 밖에 두었다. 조회 · 저장은 저장소 호출마다 짧게 끝나고, Drive 대역이 호출 시점에 트랜잭션이 없음을 기록해 E2E가 확인한다 |
| 2026-10-01 | #240 | P2 | local | 시간만 바꿀 때 과목 전체를 겹침 검사에서 빼서, 같은 과목·같은 날짜에 수업이 둘 이상이면 서로 겹치게 바뀌어도 통과한다. 전에는 수업 id만 빼서 잡혔다 (`LessonProxyService`) | 수용 | 바꾼 뒤 같은 날 같은 과목 수업끼리의 겹침을 `TeacherLessonConflictChecker.overlapAmong`으로 먼저 보고, 다른 과목 수업과의 겹침은 과목 제외로 본다. E2E로 재현 후 수정 |
| 2026-10-01 | #241 | P2 | local | 새 E2E가 테스트와 서버에서 각자 오늘 날짜를 읽어, KST 자정을 사이에 두고 돌면 날짜가 갈려 실패한다 (`SubjectScheduleUpdateTest`) | 수용 (방법은 다름) | 고정 Clock 주입 대신 자정 직전 1분은 `assumeTrue`로 건너뛴다. 서버 쪽 Clock 전환은 SubjectService의 `LocalDate.now()` 다섯 곳 전체와 별도 테스트 컨텍스트가 필요해 후속으로 둔다 |
| 2026-10-01 | #241 | P2 | local | 재생성을 내일부터 하는데 분반 과목 중복 검사(`validateSubjectDuplicate`)는 새 시작일(오늘 포함)부터 봐서, 오늘 끝나는 같은 칸의 다른 과목과 겹친다며 409를 낸다 (`SubjectService`) | 반려 | 과목 기간 자체의 겹침(분반×요일×교시당 과목 하나)을 지키는 검사라 수업 생성 시작일과 무관하다. 과목 기간은 오늘을 포함한다. 이번 diff 이전과 같은 동작 |
