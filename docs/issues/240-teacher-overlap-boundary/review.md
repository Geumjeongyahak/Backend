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

## codex 리뷰

| 회차 | 범위 | P1 | P2 | 수용 | 반려 | 남은 P1·P2 |
|---|---|---|---|---|---|---|
| 1 | `dev` 대비 전체 | 0 | 1 | 1 (af4cdcfd) | 0 | 0 |
| 2 | `dev` 대비 전체 | 0 | 0 | — | — | **0** |

지적 원문은 `harness/findings-ledger.md` 2026-10-01 #240 행.

## 커버리지 (JaCoCo, `verify.sh` 전체 1169건 뒤)

| 클래스 | 줄 | 분기 |
|---|---|---|
| `AbsenceRequestService` | 119/120 (99%) | 30/32 (93%) |
| `DailyScheduleSyncPublisher` | 7/7 (100%) | - |
| `DailyScheduleSynchronizer` | 51/51 (100%) | 4/4 (100%) |
| `FileCleanupScheduler` | 39/39 (100%) | 12/12 (100%) |
| `LessonExchangeRequestAdminViewService` | 70/70 (100%) | 16/18 (88%) |
| `LessonExchangeRequestService` | 150/151 (99%) | 42/44 (95%) |
| `LessonGenerator` | 19/19 (100%) | 8/8 (100%) |
| `LessonProxyService` | 57/61 (93%) | 15/18 (83%) |
| `LessonService` | 136/144 (94%) | 32/40 (80%) |
| `LessonSpecs` | 8/9 (88%) | - |
| `SubjectLessonScheduleService` | 26/26 (100%) | 2/2 (100%) |
| `SubjectScheduleValidator` | 47/50 (94%) | 34/38 (89%) |
| `SubjectService` | 175/185 (94%) | 59/64 (92%) |
| `TeacherLessonConflictChecker` | 30/30 (100%) | 12/14 (85%) |

못 덮은 새 코드와 이유:
- `TeacherLessonConflictChecker.TimeSlot.overlaps`의 «날짜가 다름» 갈래 2개: `overlapAmong`이 날짜별로 묶은 뒤에만 부르므로 도달하지 않는다.
- `LessonSpecs` 1줄: 암묵 생성자(정적 메서드만 있는 클래스, 기존 `UserSpecs`와 같은 모양).
- `LessonService`·`SubjectService`·`LessonProxyService`의 나머지는 이번에 안 바꾼 기존 갈래(조회 응답 조립, 상태 전이 등)다.

## 실제 PostgreSQL 18.4 검증

임시 인스턴스에 Flyway V1–V9를 적용하고 돌렸다. 테스트 준비 SQL이 날짜를 문자열로 넣어 H2만 받아 주므로
JDBC URL에 `stringtype=unspecified`를 붙였다(테스트 실행에만, 코드 변경 없음). `SubjectBaseTest` 정리 SQL은
H2 전용이라 DB 종류별로 갈랐다(33fed617).

| 묶음 | 건수 | 실패 |
|---|---|---|
| subject(겹침·일정·교사·기본·생성) · lesson · daily_schedule · teacher_assignment · request(결석 상태·교환·목록 질의 수) E2E | 162 | 0 |
| `SubjectScheduleQueryCountTest` (별도 컨텍스트, 새 DB) | 2 | 0 (질의 수 H2와 같음) |
| `AbsenceRequestOptimisticLockTest` (별도 컨텍스트, 새 DB) | 1 | 0 |

별도 Spring 컨텍스트를 띄우는 클래스는 같은 DB에 시드를 다시 넣다가 키가 겹쳐서 새 DB에서 따로 돌렸다.
기존 `PurchaseRequestConcurrencyTest`의 락 대기 409 두 건은 PostgreSQL `lock_timeout` 설정이 필요한 기존 테스트라 이번 범위 밖이다.

## 검사

`scripts/harness/verify.sh` 통과 (H2, 1169건).
