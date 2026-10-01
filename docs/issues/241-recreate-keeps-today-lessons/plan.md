# 과목 수업 재생성은 내일부터 — 당일 수업·DailySchedule·출석을 건드리지 않는다

- 이슈: #241
- 브랜치: `fix/241-recreate-keeps-today-lessons` (#240 병합 커밋 `532f2676` 위로 rebase)

## 지금 무엇이 일어나나 (#240 병합 뒤 코드)

#240이 수업 일괄 처리를 `lesson/service/schedule/`로 옮겼지만 흐름은 같다.

1. `SubjectService.updateSchedule`이 기간·요일이 바뀌면 `SubjectScheduleRecreatedEvent(effectiveFrom = today,
   startAt = max(today, newStartAt))`를 발행한다
2. `SubjectEventHandler`(BEFORE_COMMIT) → `SubjectLessonScheduleService.recreateLessons`
   → `deleteFutureLessons(from = today)`가 오늘 이후 `SCHEDULED` 수업을 soft delete하고
   `DailyScheduleSyncPublisher`가 (분반, 날짜)마다 동기화를 요청한다
3. `LessonEventHandler`는 **동기 `@EventListener`**라 바로 `DailyScheduleSynchronizer.synchronize`가 돈다.
   그 날짜에 활성 수업이 없으면 `deleteOrphan`이 DailySchedule과 교사·학생 출석을 soft delete한다
4. 그다음 `createLessons(startAt ~ endAt)`가 새 수업을 만든다

그래서 두 경우에 당일 기록이 사라진다.

| 경우 | 결과 |
|---|---|
| 기간을 미래로 옮긴다 (dev 2026-09-30 20:08, 9월 → 10/1–10/30) | 당일 수업이 지워지고 다시 안 만들어진다. DailySchedule·출석 삭제 |
| 당일이 새 기간 안에 그대로 있다 (예: 종료일만 연장) | 당일 수업을 지웠다가 새로 만든다. 3에서 DailySchedule·출석이 지워지고, 4의 동기화는 `IsDeletedFalse`로 찾으므로 **새 DailySchedule을 만든다**. 지워진 출석은 되살아나지 않는다 |

## 기준

**재생성(기간·요일 변경)은 내일부터 적용한다. 오늘과 그 이전 수업은 그대로 둔다.**

- 오늘은 수업이 진행 중이거나 출석이 쌓이는 날이다. 기간 변경은 「다음 수업부터」로 읽는다
- 내일부터 새 시작일 전날까지의 예정 수업은 지금처럼 지운다. 새 기간 밖이다
- 시간·교시만 바꾸는 경우(`SubjectScheduleUpdatedEvent`)는 그대로 오늘부터다. 수업을 지우지 않고 시간만
  바꾸므로 기록이 사라지지 않는다

## 무엇을 바꾸나

`SubjectService.updateSchedule` 한 곳이다.

```java
LocalDate today = LocalDate.now();
LocalDate changeFrom = recreateLessons ? today.plusDays(1) : today;   // 재생성은 당일을 건드리지 않는다 (#241)
LocalDate lessonStartAt = max(changeFrom, newStartAt);
```

| 자리 | 지금 | 바꾼 뒤 |
|---|---|---|
| `validator.validateFutureLessonsChangeable(subjectId, …)` | `today` | `changeFrom` — 바꿀 수업만 검사한다. 오늘 수업일지를 쓴 과목도 다음 기간으로 옮길 수 있다 |
| `validator.validateNoTeacherConflictForSchedule(…, today, startAt, …)` | `startAt = max(today, newStartAt)` | `startAt = lessonStartAt`. `today` 인자(시간만 바꿀 때 쓰는 것)는 그대로 |
| `SubjectScheduleRecreatedEvent` | `effectiveFrom = today`, `startAt = max(today, newStartAt)` | `effectiveFrom = changeFrom`, `startAt = lessonStartAt` |

**안 바꾸는 것.** `SubjectLessonScheduleService`·`LessonGenerator`·`DailyScheduleSynchronizer`·`SubjectScheduleValidator`.
lesson 쪽은 받은 `from`부터 지우는 일만 하고, 날짜 기준은 발행자 한 곳에 둔다. `startAt > endAt`이면
`LessonGenerator.lessonDates`가 빈 목록을 돌려주므로 따로 막을 필요가 없다.

도메인 경계: 기존 이벤트(Subject 발행 → Lesson 수신) 그대로. 새 이벤트·Proxy 메서드 없음.
스키마: 변경 없음, 마이그레이션 없음. 권한: 새 엔드포인트 없음.

## 실패 경로

| 상황 | 결과 |
|---|---|
| 요일을 오늘 요일로 바꾼다 (오늘 수업이 원래 없었다) | 오늘 수업은 만들지 않는다. 내일 이후부터 생성 |
| 오늘 수업이 있는데 요일을 다른 요일로 바꾼다 | 오늘 수업은 옛 요일 그대로 남는다. 내일부터 새 요일로 생성 |
| 새 기간이 오늘 끝난다 (`newEndAt == today`) | 내일 이후 수업만 지우고 생성은 없다 (`lessonDates`가 빈 목록) |
| 오늘 수업에 수업일지·결석 요청·교환 요청이 있다 | 재생성은 막지 않는다(오늘을 안 건드리므로). 시간만 바꾸는 경우는 지금처럼 409 |
| 같은 과목을 동시에 수정 | 이번 변경과 무관. 지금 동작 그대로 |

질의: 새 질의는 없고, 기존 질의의 시작일만 하루 늦어진다. `SubjectScheduleQueryCountTest`는 2099년 과목이라 영향이 없다.

## 안 하는 것

- 담당 교사 해제(`SubjectTeacherUnassignedEvent`)와 과목 삭제(`SubjectDeletedEvent`)도 `today`부터 지운다.
  같은 성질의 위험이지만 이슈 범위 밖이고, 과목을 삭제할 때 당일 수업을 남길지는 따로 정해야 한다. PR `리뷰어에게`에 후속 이슈 후보로 적는다
- 지우는 동기화가 출석을 soft delete하고 새 동기화가 되살리지 않는 `DailyScheduleSynchronizer`의 성질 자체.
  이번 기준으로 재생성 경로에서는 당일이 빠지므로 드러나지 않는다. 고치려면 daily_schedule 도메인 설계가 필요하다
- 기간별 과목 복사 흐름, 칸 저장 부분 반영 (이슈 본문대로 Frontend 이슈)

## 테스트 (먼저 쓰고 실패를 본다)

`SubjectScheduleUpdateTest`(#240이 나눈 `/schedule` E2E)에 넣는다. 날짜는 오늘 기준 상대값이고, 과목은
`startAt = today`, 요일 = 오늘 요일, 시드와 겹치지 않는 시간대로 만든다. 만들 때 오늘 수업과 DailySchedule·교사 출석이 생긴다.

| 층 | 테스트 | 고치기 전 |
|---|---|---|
| E2E | 기간을 `today+1 ~ today+30`으로 옮기면 오늘 수업·DailySchedule·교사 출석이 `is_deleted = FALSE`로 남고, 새 수업은 `today+7`부터다 | 실패 (오늘 수업 삭제) |
| E2E | 종료일만 연장하면 오늘 수업이 같은 id로 남고 DailySchedule·교사 출석도 같은 id로 그대로다. 오늘 날짜 활성 수업은 1건 | 실패 (오늘 수업을 다시 만들고 출석 삭제) |

학생 출석은 시드의 분반 1 학생 2명(`student_classrooms`) 몫이 활성 2건으로 남는지 본다.

단위 테스트는 두지 않는다. 계산이 `today.plusDays(1)` 한 줄이고, `SubjectService`를 목으로 감싸 이벤트 값을
되읽는 테스트는 위 E2E가 보는 것보다 덜 본다. 이슈의 「통합」 항목은 위 E2E가 실제 이벤트 → `recreateLessons` →
동기화를 다 지나므로 거기서 덮는다.

## 작업과 판정

| # | 커밋 | 판정 |
|---|---|---|
| 1 | `test(subject): 기간 변경 시 당일 수업·출석 보존 E2E 추가` | 두 테스트가 지금 코드에서 위 이유로 실패 |
| 2 | `fix(subject): 과목 기간·요일 변경 시 수업 재생성을 내일부터 적용` | `./gradlew test --tests '*SubjectSchedule*'` 통과 |
| 3 | 리뷰 기록 (`review.md`, ledger) | `scripts/harness/verify.sh` 통과, codex 리뷰 P1·P2 없음 |
