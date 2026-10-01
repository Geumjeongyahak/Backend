# #240 검토 기록

## A·B 질의 수 측정

측정: `RequestListQueryCountTest` (H2, `hibernate.generate_statistics`의 `PrepareStatementCount`).
목록은 HTTP 요청 한 번 전체(인증 필터 질의 포함), 대시보드는 `LessonExchangeRequestAdminViewService.getDashboard()` 한 번이다.
같은 테스트를 수정 전 코드(`d4029785`)와 수정 뒤 코드에서 돌렸다. 행은 교사 둘(teacher01·teacher02)이 나눠 만든다.

| 대상 | 수정 전 2행 | 수정 전 6행 | 수정 뒤 2행 | 수정 뒤 6행 |
|---|---|---|---|---|
| 결석 요청 목록 `GET /api/v1/absence-requests` | 9 | 13 | 7 | 7 |
| 수업 교환 요청 목록 `GET /api/v1/lesson-exchange-requests` | 6 | 6 | 5 | 5 |
| 관리자 교환 대시보드 `getDashboard()` | 17 | 25 | 13 | 13 |

- 결석 목록: 행마다 `DailySchedule`를 따로 읽었다(+1/행). `default_batch_fetch_size: 100`으로 IN 한 번에 묶인다.
- 교환 목록: 지연 로딩되는 연관은 신청자(`User`)뿐이라 신청자 수만큼 늘었다(테스트는 신청자 2명이라 행 수와 무관하게 보인다). 배치 뒤 1번으로 묶여 1 줄었다.
- 대시보드: 검토 대기 상위 10건마다 `count` 2번(+2/행)이 `request.id IN (...) group by request.id, status` 1번으로 바뀌었다.
  남은 13 중 10은 상태별 개수(요청 6 + 제안 4)라 행 수와 무관하다. 이번 범위 밖.
- 판정: 수정 뒤에는 2행과 6행의 질의 수가 같다 (테스트가 같음을 단정한다).

B — 만료 스케줄러:

| 작업 | 수정 전 | 수정 뒤 |
|---|---|---|
| 결석 요청 만료 | 대상 조회 1 + 행마다 UPDATE N | 일괄 UPDATE 1 |
| 교환 요청 만료 | 대상 조회 1 + 행마다 UPDATE N + 제안 목록 지연 로딩 N + 제안 UPDATE | 일괄 UPDATE 2 (제안 닫기 → 요청 만료) |

만료 UPDATE는 `version`·`updated_at`도 올린다. 그래서 승인이 요청을 읽은 뒤 만료가 먼저 커밋되면
승인 쪽 flush가 `ObjectOptimisticLockingFailureException`이 되어 409 `BIZ004`로 끝난다
(`AbsenceRequestOptimisticLockTest`).
