# 과목 수정은 오늘 수업이 시작 전이면 오늘부터, 시작했으면 내일부터 — 진행·종료된 당일 수업·DailySchedule·출석을 건드리지 않는다

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
| 당일이 새 기간 안에 그대로 있다 (예: 종료일만 연장) | 당일 수업을 지웠다가 **새 id로** 다시 만든다. DailySchedule·출석은 지워졌다가 #240의 `DailyScheduleSynchronizer`가 `restore()`로 되살린다 (첫 계획의 「되살아나지 않는다」는 #240 이전 코드 기준이라 틀렸다) |

## 개정 3 (사람 결정, 2026-10-01) — 수업 시작 시각 기준

「항상 내일부터」 대신 **지금 시각과 그 과목의 수업 시작 시각**으로 정한다.

- 기준 시각 = 과목 시작 시각. 일정 변경은 옛 시작 시각과 새 시작 시각 중 이른 쪽
- 지금이 기준 시각 **전**이면 오늘부터, **같거나 지났으면** 내일부터
- 적용 경로: 기간·요일 재생성, **시간·교시만 변경**, 교사 해제·교체·미배정 배정, 과목 삭제. 과목 생성만 그대로 오늘부터
- 예: 19:20 수업을 18:00에 고치면 오늘 수업도 바뀐다. 20:08(9/30 사고)에 고치면 내일부터
- 개정 2에서 「의도」로 적었던 「오늘 요일로 바꾸거나 아침에 교사를 배정해도 오늘 수업이 안 생긴다」는 시작 전이면 해소된다
- 시간만 변경도 수업 시작 뒤에는 내일부터라, 끝난 수업의 시간·봉사 시간이 바뀌지 않는다. 아침에 오늘 수업 시간을 바로잡는 쓰임은 시작 전이라 그대로 된다

구현: `SubjectService.lessonChangeFrom(LocalTime startTime)` 하나. `LessonService`·lesson 모듈은 그대로.
`TimeSlot`(lesson 도메인의 겹침 판정 record)은 가져다 쓰지 않는다. 과목의 시작 시각만 있으면 되고, 다른 도메인 내부 타입을 끌어오지 않는다.

테스트 (`SubjectTodayLessonTest`, 고정 시각 오늘 12:00): 기존 6건은 06:00 수업이라 「시작 뒤」 경우로 남는다.
추가: 18:00 수업(시작 전)에 기간을 옮기면 오늘 수업도 지워진다 / 교사 교체가 오늘 수업에 반영된다 / 요일을 오늘 요일로 바꾸면 오늘 수업이 생긴다.
06:00 수업(시작 뒤)에 시간만 바꾸면 오늘 수업 시간은 그대로이고 다음 주부터 바뀐다.

## 개정 2 (#240 창 리뷰 피드백, 2026-10-01)

같은 「오늘부터 지운다」가 두 경로에 더 있다. 9/30에도 교사 해제가 9건 있었고, 그날이 수업 요일이 아니어서 피해가 없었을 뿐이다.

| 경로 | 지금 | 바꾼 뒤 |
|---|---|---|
| 담당 교사 해제 `PATCH /teacher {teacherId:null}` → `SubjectTeacherUnassignedEvent(today)` | 당일 수업·DailySchedule·출석 삭제 | 내일부터 삭제. 당일 수업은 옛 교사로 남는다 |
| 과목 삭제 `DELETE` → `SubjectDeletedEvent(today)` | 같음 | 내일부터 삭제. 당일 수업은 비활성 과목에 달린 채 남는다 (과목은 soft delete가 아니라 `deactivate`) |
| 담당 교사 교체 A→B → `SubjectTeacherAssignedEvent(today)` | 당일 수업 교사가 B로 바뀌고, DailySchedule 담당 교사와 그날 봉사 기록도 B로 넘어간다. 수업 중에 바꾸면 A가 한 수업이 B 몫이 된다 | 내일부터 교체. 당일 대타는 수업 교환 요청으로 처리한다 |
| 미배정 과목에 교사 배정 → `SubjectCreatedEvent(max(today, startAt))` | 오늘부터 생성 | 내일부터 생성. 같은 날 해제(당일 수업 남음) → 재배정이면 오늘 수업이 둘이 되는 것을 막는다. 「미래 수업이 있나」(`existsFutureActiveLessonBySubjectId`)도 내일부터 본다. 안 그러면 남은 당일 수업 하나 때문에 내일부터의 수업이 안 만들어진다 |
| 시간·교시만 변경 → `SubjectScheduleUpdatedEvent(today)` | 오늘 수업 시간도 바뀐다 | **그대로 오늘부터.** 지우지 않고, 아침에 오늘 수업 시간을 바로잡는 쓰임이 있다 |
| 과목 생성 → `SubjectCreatedEvent(max(today, startAt))` | 오늘부터 생성 | 그대로. 지울 것이 없다 |

**적용 시작일은 `SubjectService` 한 곳에서 정한다.** `Clock` 빈(Asia/Seoul)을 주입하고
`lessonChangeFrom()` = `LocalDate.now(clock).plusDays(1)` 하나를 두어 위 네 경로와 재생성이 같이 쓴다. 날짜 경계는 달력 자정이다(사람 결정).
`SubjectService`의 인자 없는 `LocalDate.now()`·`LocalDateTime.now()`는 모두 `clock`으로 바꾼다 (diff-signals #226).

요일을 오늘 요일로 바꾸면 오늘 수업은 새로 생기지 않는다(내일부터 생성). 의도이며 PR `리뷰어에게`에 적는다.

테스트: #241 E2E를 `SubjectTodayLessonTest`로 옮기고 `@TestConfiguration`의 `@Primary` 고정 `Clock`(오늘 12:00 KST)을 쓴다.
테스트와 서버가 같은 시각을 보므로 `assumeTrue` 자정 건너뛰기를 지운다. 이 클래스 하나만 별도 컨텍스트가 뜬다.
추가 E2E: 교사 해제 · 과목 삭제 · 교사 교체 뒤 당일 수업(교사 포함)·DailySchedule·출석 유지, 해제 → 같은 날 재배정 시 내일부터 수업 생성·당일 1건.

## 기준 (첫 계획)

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

- ~~담당 교사 해제·과목 삭제는 범위 밖~~ → 개정 2에서 포함
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
