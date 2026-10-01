# 과목 수업 재생성은 내일부터 — 당일 수업·DailySchedule·출석을 건드리지 않는다

- 이슈: #241
- 브랜치: `fix/241-recreate-keeps-today-lessons`

## 선행 조건 — #240 병합 뒤 시작

#240이 lesson 도메인의 겹침 판정·수업 생성/재생성·DailySchedule 동기화 이벤트를 모듈로 재구성한다
(`LessonService`·`LessonProxyService`·`LessonRepository` 구조 변경). 구현은 **#240이 dev에 병합된 뒤 이
브랜치를 rebase해서** 시작한다. 그때 아래 「지금 무엇이 일어나나」의 흐름(재생성 → 삭제 → 동기화 →
`deleteOrphanDailySchedule`)이 새 구조에서도 같은지 다시 확인하고, 다르면 이 계획을 고쳐 게이트를 다시 받는다.
이 계획의 변경은 `SubjectService.updateSchedule`(발행자)에만 있으므로 #240과 겹치지 않을 것으로 본다.

## 지금 무엇이 일어나나 (2026-10-01, #240 병합 전 코드 기준)

`SubjectService.updateSchedule`이 기간·요일이 바뀌면 `SubjectScheduleRecreatedEvent(effectiveFrom = today,
startAt = max(today, newStartAt))`를 발행한다. `LessonService.recreateSubjectScheduledLessons`는
`effectiveFrom` 이후 `SCHEDULED` 수업을 soft delete하고(`deleteFutureSubjectScheduledLessons`) `startAt`부터
다시 만든다. 지운 날짜마다 `LessonDailyScheduleSyncRequestedEvent`가 나가고, 그 분반·날짜에 활성 수업이
없으면 `deleteOrphanDailySchedule`이 DailySchedule과 교사·학생 출석을 soft delete한다.

그래서 두 경우에 당일 기록이 사라진다.

| 경우 | 결과 |
|---|---|
| 기간을 미래로 옮긴다 (dev 2026-09-30 20:08, 9월 → 10/1–10/30) | 당일 수업이 지워지고 다시 안 만들어진다. DailySchedule·출석 삭제 |
| 당일이 새 기간 안에 그대로 있다 (예: 종료일만 연장) | 당일 수업을 지웠다가 새로 만든다. 지우는 순간 DailySchedule·출석이 soft delete되고, 새 수업으로 동기화할 때 **지워진 출석은 되살아나지 않는다**(`findByClassroomIdAndLessonDateAndIsDeletedFalse`라 새 DailySchedule을 만든다) |

## 기준

**재생성(기간·요일 변경)은 내일부터 적용한다. 오늘과 그 이전 수업은 그대로 둔다.**

- 오늘은 이미 수업이 진행 중이거나 출석이 쌓이는 날이다. 기간 변경은 「다음 수업부터」의 의미로 읽는다
- 내일부터 새 시작일 전날까지의 예정 수업은 지금처럼 지운다. 새 기간 밖이기 때문이다
- 시간·교시만 바꾸는 경우(`SubjectScheduleUpdatedEvent`)는 그대로 오늘부터다. 수업을 지우지 않고 시간만
  바꾸므로 기록이 사라지지 않는다

## 무엇을 바꾸나

| 파일 | 변경 |
|---|---|
| `SubjectService.updateSchedule` | `changeFrom = recreateLessons ? today.plusDays(1) : today`. 재생성 이벤트의 `effectiveFrom`은 `changeFrom`, `startAt`은 `max(changeFrom, newStartAt)`. 교사 겹침 검증(`validateNoTeacherConflictForSchedule`)의 시작일도 `max(changeFrom, newStartAt)`, 운영 기록 검증(`validateFutureLessonsChangeable`)도 `changeFrom`부터 — **바꿀 수업만 검사한다**. 그래야 오늘 수업일지를 이미 쓴 과목도 다음 기간으로 옮길 수 있다 |

`LessonService`는 바꾸지 않는다. `recreateSubjectScheduledLessons`·`deleteFutureSubjectScheduledLessons`는
받은 `effectiveFrom`부터 지우는 일만 하고, 날짜 기준은 발행자(`SubjectService`)가 정한다. 같은 기준을 두
곳에 두지 않는다. #240이 고치는 교사 겹침 질의 3개와 그 호출 줄은 건드리지 않는다.

도메인 경계: 기존 이벤트(`SubjectScheduleRecreatedEvent`, Subject 발행 → Lesson 수신) 그대로. 새 이벤트·Proxy 메서드 없음.
스키마: 변경 없음. 마이그레이션 없음. 권한: 새 엔드포인트 없음.

## 실패 경로

| 상황 | 결과 |
|---|---|
| 요일을 오늘 요일로 바꾼다 (오늘 수업이 원래 없었다) | 오늘 수업은 만들지 않는다. 내일 이후부터 생성. 당일 수업을 수정 중에 새로 끼워 넣는 것은 요구가 아니다 |
| 오늘 수업이 있는데 요일을 다른 요일로 바꾼다 | 오늘 수업은 남는다(옛 요일). 내일부터 새 요일로 생성 |
| 새 기간이 오늘에 끝난다 (`newEndAt == today`) | `startAt(내일) > endAt` → 지우기만 하고 생성 없음 (기존 분기) |
| 오늘 수업에 수업일지가 있다 | 재생성은 막지 않는다(오늘은 안 건드림). 시간만 변경은 지금처럼 409 |
| 같은 과목에 동시 수정 | 이번 변경과 무관. 지금 동작 그대로 |

질의: 새 질의 없음. 기존 질의의 시작일만 하루 늦어진다.

## 안 하는 것

- 담당 교사 해제(`SubjectTeacherUnassignedEvent`)·과목 삭제(`SubjectDeletedEvent`)도 `today`부터 지운다.
  같은 성질의 위험이지만 이슈 범위 밖이고, 과목 삭제 시 당일 수업을 남길지는 따로 정해야 한다. 후속 이슈 후보로 PR에 적는다
- 기간별 과목 복사 흐름, 칸 저장 부분 반영 (이슈 본문대로 Frontend 이슈)

## 테스트 (먼저 쓰고 실패를 본다)

날짜는 오늘 기준 상대값. 과목은 `startAt = today`, 요일 = 오늘 요일, 시드와 안 겹치는 시간대로 만든다.
생성 시 오늘 수업과 DailySchedule·출석이 생긴다.

| 층 | 테스트 | 고치기 전 |
|---|---|---|
| E2E (`SubjectUpdateTest`) | 기간을 `today+1 ~ today+30`으로 옮기면 오늘 수업·DailySchedule·교사 출석이 `is_deleted = FALSE`로 남고, 새 수업은 `today+7`부터 | 실패 (오늘 수업 삭제) |
| E2E | 종료일만 연장하면 오늘 수업은 같은 행(id)으로 남고 DailySchedule·출석도 그대로, 오늘 날짜 활성 수업은 1건 | 실패 (오늘 수업 재생성, 출석 삭제) |

단위 테스트는 두지 않는다. 계산이 `today.plusDays(1)` 한 줄이고, 목으로 이벤트 값을 되읽는 테스트는
E2E가 이미 보는 것을 덜 본다. 「통합」 항목은 위 E2E가 실제 이벤트 → `recreateSubjectScheduledLessons` →
DailySchedule 동기화를 다 지나므로 거기서 덮는다.

판정: `./gradlew test --tests '*SubjectUpdateTest*'` 통과, 이어서 `scripts/harness/verify.sh` 통과.
