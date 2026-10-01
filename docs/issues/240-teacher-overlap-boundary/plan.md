# 교사 수업 겹침 판정을 한 곳으로 모으고 맞닿은 시간을 겹침으로 보지 않게 한다

이슈: #240 [FIX] 맞닿은 교시를 교사 수업 겹침으로 잘못 판정함
되돌리기 1회차: 사용자 요청으로 범위를 넓혔다 — 겹침 판정·수업 생성·DailySchedule 동기화를 모듈로 묶는다.
사용자 결정(2026-10-01): N+1 조사에서 나온 요청 목록 N+1(A)·스케줄러 정리(B)·커버리지 도구(C)는 새 이슈를 만들지 않고 이 PR에 넣는다.

## 원인

`LessonRepository`의 교사 겹침 파생 질의 셋이 경계를 포함해 비교한다
(`StartTimeLessThanEqualAndEndTimeGreaterThanEqual` → `기존.start <= 새.end AND 기존.end >= 새.start`).
12:20–13:00 수업이 있는 교사에게 13:00–13:30을 넣으면 겹침이 된다. 같은 분반 과목 중복 검사
(`SubjectService.hasActualScheduleConflict`)는 이미 경계를 뺀다.

판정 규칙이 질의 **이름**에 들어 있고 거의 같은 질의가 셋, 호출자가 일곱이라, 비교 기호 하나를
바꾸는 데 일곱 곳을 고쳐야 한다. 이번에 한 곳으로 모은다.

## 지금 코드의 냄새 (이번 범위)

| # | 냄새 | 위치 |
|---|---|---|
| 1 | 거의 같은 파생 질의 3개 (제외 대상만 없음/수업 id/과목 id) | `LessonRepository:90-106` |
| 2 | 날짜마다 `exists` 질의 1번 (N+1). 1년 과목이면 52번 | `LessonProxyService:145-220`, `LessonService.createLessonsFromSubject:258` |
| 3 | 오버로드 2개가 «기존 시간으로 볼지 새 시간으로 볼지»만 다름 | `LessonProxyService.existsTeacherConflictForFutureSubjectScheduledLessons` ×2 |
| 4 | 수업 하나 바뀔 때마다 DailySchedule 동기화 이벤트 1건. 같은 (분반, 날짜)를 여러 번 동기화 | `LessonService.publishDailyScheduleSync` (15곳) |
| 5 | lesson 도메인이 subject의 Repository를 직접 주입 (도메인 경계 위반) | `LessonService:48` `SubjectRepository` |
| 6 | 동기화 1번마다 학생 수만큼 출석 조회 (N+1). 과목 저장 1번 = W(수업 날짜 수) × (8 + 2S(학생 수)) 질의, W=16·S=15면 약 600 | `DailyScheduleService.initializeStudentAttendances:974-982` |
| 7 | 교사 배정 묶음(K개 과목)이 같은 (분반, 날짜)를 K번 동기화 | `TeacherAssignmentService.assignSchedule:37-40` → `SubjectService.assignTeacher` |

범위 밖 (다른 이슈):
- 기간 변경 시 «오늘부터 전부 지우고 다시 만들기» → #241 (이 PR 병합 뒤 rebase)
- 자동 생성 중 겹치는 날짜를 조용히 건너뛰는 정책 → 동작은 그대로 두고 건너뛴 날짜를 로그에 남긴다. 실패로 바꿀지는 정책 결정이 필요하다
- QueryDSL 도입 안 함. 레포의 동적 조회 방식인 `Specification`(분반·회의록·사용자·학생 4곳)을 따른다

## 설계

### 판정 규칙

시간 구간을 반열린 구간 `[start, end)`로 본다. `겹침 ⇔ 기존.start < 새.end AND 기존.end > 새.start`

### 모듈 — `domain/lesson/service/schedule/`

```
TeacherLessonConflictChecker   겹침 판정의 유일한 입구
LessonGenerator                기간·요일 → 날짜 계산, 수업 생성(겹치는 날짜 제외)
DailyScheduleSyncCollector     바뀐 (분반, 날짜)를 모아 한 번씩만 동기화 이벤트 발행
```

**TeacherLessonConflictChecker**

```java
// 겹치는 날짜 목록을 한 번의 질의로 돌려준다
Set<LocalDate> findConflictDates(Long teacherId, Collection<LocalDate> dates,
                                 LocalTime start, LocalTime end, ConflictExclusion exclude);
boolean hasConflict(...)   // = !findConflictDates(...).isEmpty()

record ConflictExclusion(Long lessonId, Long subjectId) { NONE, ofLesson(id), ofSubject(id) }
```

질의는 레포의 동적 조회 방식인 `Specification`으로 만든다 (`users/repository/specification/UserSpecs` 형태).
`LessonRepository`가 `JpaSpecificationExecutor<Lesson>`을 더 상속한다.

```java
// domain/lesson/repository/specification/LessonSpecs.java
public static Specification<Lesson> isActive()                          // isDeleted = false
public static Specification<Lesson> taughtBy(Long teacherId)            // teacher.id = ?
public static Specification<Lesson> onDates(Collection<LocalDate> d)     // date in (?)
public static Specification<Lesson> overlapsTime(LocalTime s, LocalTime e) // startTime < e and endTime > s
public static Specification<Lesson> excludingLesson(Long lessonId)      // id <> ?
public static Specification<Lesson> excludingSubject(Long subjectId)    // subject.id <> ?
```

checker가 조합해 `lessonRepository.findAll(spec)` 한 번으로 겹치는 수업을 읽고 날짜만 뽑는다.
읽은 수업에서 날짜 외 연관(교사·과목)은 건드리지 않아 지연 로딩 질의가 생기지 않는다.
비교 규칙(`<`, `>`)은 `overlapsTime` 한 곳에만 있다.

기존 파생 질의 3개는 지운다.

**제외 의미 확인.** `existsTeacherConflictForFutureSubjectScheduledLessons`는 지금 수업마다 그 수업 id를 빼고
검사한다. 한 과목은 한 날짜에 수업이 하나라서 «그 과목의 수업을 뺀다»와 같다 → `ofSubject(subjectId)`로 바꾼다.
같음을 E2E로 고정한다(아래 테스트 6).

**LessonGenerator**

`createLessonsFromSubject`의 날짜 계산·겹침 제외·저장을 옮긴다. 겹치는 날짜는 `findConflictDates` 한 번으로 구하고,
건너뛴 날짜가 있으면 날짜 목록을 `warn`으로 남긴다(지금은 날짜마다 한 줄).

**DailyScheduleSyncCollector**

수업 변경 메서드는 이벤트를 바로 발행하지 않고 (분반, 날짜)를 collector에 등록한다. collector는 키를 **트랜잭션 단위**로
모은다(`TransactionSynchronizationManager`에 `Set`을 묶고, 처음 등록할 때 `beforeCommit` 동기화를 건다). 커밋 직전에
키마다 `LessonDailyScheduleSyncRequestedEvent`를 한 번 발행한다. 트랜잭션 단위라서 교사 배정 묶음(냄새 7)처럼
한 트랜잭션 안에서 과목 여러 개를 바꿔도 같은 키는 한 번만 동기화된다.

- 이벤트 타입·수신자(`daily_schedule.LessonEventHandler`)는 그대로다. 새 이벤트는 만들지 않는다
- 트랜잭션 밖에서 등록되면(없어야 정상) 즉시 발행한다 — 지금 동작과 같다
- 커밋 직전 발행이라 동기화 예외는 지금처럼 트랜잭션 전체를 롤백한다

**출석 초기화 일괄 처리 (냄새 6)** — 이벤트 수신 쪽, daily_schedule 도메인

`initializeStudentAttendances`가 학생마다 조회하던 것을 일정의 출석을 한 번에 읽어(`findAllByDailyScheduleId…`)
메모리에서 빠진 학생만 골라 `saveAll`한다. 동기화 1번의 질의 수: `4 + S` → `5`.

### 도메인 경계

| 통로 | 지금 | 바뀐 뒤 |
|---|---|---|
| lesson → subject 조회 | `SubjectRepository` 직접 주입 | `SubjectProxyService.getById` |
| subject → lesson 겹침 조회 | `LessonProxyService`의 겹침 메서드 3개 | `LessonProxyService`가 `TeacherLessonConflictChecker`에 위임. 메서드 수 3 → 2 (오버로드 통합) |
| request → lesson 겹침 조회 | `LessonProxyService.existsActiveLessonConflict` | 그대로 (내부만 위임) |
| lesson → daily_schedule | 수업마다 이벤트 | (분반, 날짜)마다 이벤트 1건 |

`SubjectService`는 오버로드 통합으로 호출 한 줄만 바뀐다(`validateNoTeacherConflictForSchedule`).
권한 변경 없음. 스키마 변경은 작업 8의 `version` 컬럼 둘.

### Flyway

현재 마지막 버전 `V8`. 병렬 작업끼리 번호를 미리 나눈다: **#240 → V9**, #241 → V10, #221 → V11, #242 → V12.
이번에 쓰는 것은 작업 8의 `V9__add_request_version_columns.sql` 하나다.
겹침 질의(교사 + 날짜 IN)는 기존 `idx_lessons_teacher_id`·`idx_lessons_date`로 충분하다(교사 한 명의 1년 수업 ≈ 150건). 인덱스는 더하지 않는다.

## 작업

### 1. 실패하는 E2E를 먼저 쓴다 — `test(subject)`, `test(lesson)`

날짜는 `LocalDate.now()` 기준 미래 요일로 만든다.

| # | 파일 | 테스트 | 기대 |
|---|---|---|---|
| 1 | `e2e/subject/SubjectUpdateTest` | 같은 교사 2교시 12:20–13:00이 있을 때 3교시 기간 변경으로 13:00–13:30 (재생성 경로) | 200, 3교시 수업 생성 |
| 2 | 〃 | 시간만 13:00–13:30으로 변경 (시간만 변경 경로) | 200 |
| 3 | 〃 | 12:59 시작 (1분 겹침) | 409 `BIZ-05-002` |
| 4 | 〃 | 맞닿은 과목이 있는 교사를 `PATCH /teacher`로 배정 | 200, 기간 안 모든 날짜에 수업 생성 (건너뜀 0) |
| 5 | 〃 | 기간 중 **한 날짜만** 다른 과목과 겹치게 하고 재생성 | 409 (여러 날짜 중 하나라도 겹치면 잡는다 — IN 질의 확인) |
| 6 | 〃 | 같은 과목의 기존 수업과는 충돌하지 않는다 (시간만 변경) | 200 (제외 의미 유지) |
| 7 | `e2e/lesson/LessonCreateTest` | 같은 교사 12:20–13:00 뒤 13:00–13:30 생성 / 12:59–13:30 생성 | 201 / 409 |
| 8 | `e2e/lesson/LessonUpdateTest` | 수업 시간을 앞 수업 종료 시각에 맞닿게 수정 | 200 |

판정: 1·2·4·7(201)·8이 수정 전 코드에서 실패하는 것을 본다. 3·5·6·7(409)은 수정 전에도 통과한다(회귀 방지).

### 2. 단위 테스트 — `test(lesson)`

`unit/lesson/`

- `LessonGeneratorTest`: 기간·요일 → 날짜 계산(시작·종료일 포함, 요일 없는 기간 → 0건), 겹치는 날짜 제외
- `DailyScheduleSyncCollectorTest`: 같은 (분반, 날짜) 여러 번 → 이벤트 1건, 다른 키 → 각각 1건

겹침 판정 규칙(`LessonSpecs.overlapsTime`)은 Criteria 조건이라 DB 없이 검증할 수 없다. E2E 1–8이 맡는다.

### 3. 겹침 판정 통합 — `refactor(lesson)` + `fix(lesson)`

- `TeacherLessonConflictChecker`, `ConflictExclusion`, `LessonSpecs` 추가, `LessonRepository`에 `JpaSpecificationExecutor<Lesson>` 상속
- 호출자 일곱 곳을 checker로 바꾸고 파생 질의 3개 삭제
- `LessonProxyService` 오버로드 통합, `SubjectService` 호출 한 줄 수정

판정: 작업 1의 E2E 전부 통과.

### 4. 수업 생성 분리 — `refactor(lesson)`

- `LessonGenerator`로 `createLessonsFromSubject`의 본문을 옮긴다. `LessonService.createLessonsFromSubject`는 위임만 한다
  (`SubjectEventHandler` 호출부는 그대로)
- `SubjectRepository` 주입을 `SubjectProxyService`로 바꾼다

판정: `LessonGeneratorTest`, 기존 `SubjectUpdateTest`·`SubjectCreate*` E2E 통과.

### 5. 동기화 이벤트 모으기 — `refactor(lesson)`

- `DailyScheduleSyncCollector`로 15곳의 `publishDailyScheduleSync`를 바꾼다
- `DailyScheduleService.initializeStudentAttendances`를 일괄 조회 + `saveAll`로 바꾼다 — `fix(daily-schedule)`

판정: `DailyScheduleSyncCollectorTest`(같은 트랜잭션 안 중복 키 1건, 트랜잭션 밖 즉시 발행), `e2e/daily_schedule/**`,
`e2e/teacher_assignment/**` 통과. 질의 수 측정값이 수정 전보다 줄었다.

### 6. 커버리지 검토와 빈 곳 메우기 — `test(lesson)` (작업 9의 JaCoCo로 측정)

목표: 이번에 바뀐 코드의 모든 갈래를 실제 환경과 같은 조건에서 한 번 이상 지난다.

1. **커버리지 측정.** 작업 9에서 넣은 JaCoCo 리포트로 본다. A·B에서 바뀐 클래스도 같이 본다. 볼 대상:
   `TeacherLessonConflictChecker`, `LessonSpecs`, `LessonGenerator`, `DailyScheduleSyncCollector`,
   `LessonProxyService`의 겹침 메서드, `LessonService`의 생성·수정·재생성·삭제 메서드, `SubjectService.validateNoTeacherConflictForSchedule`
2. **판정.** 위 클래스의 줄·분기 커버리지 표를 `review.md`에 남긴다. 덮이지 않은 분기는 테스트를 더하거나,
   못 덮는 이유(도달 불가 등)를 적는다. 목표치는 새 클래스 분기 100%, 바뀐 메서드 분기 90% 이상.
3. **덮을 경계 (단위·E2E 나눔)**

| 경계 | 층 |
|---|---|
| 맞닿음(끝=시작), 시작=끝 반대쪽(새 끝=기존 시작), 1분 겹침, 완전 포함, 완전 감쌈, 같은 시간 | E2E (Specs) |
| 날짜 목록이 비었을 때 (기간 안에 그 요일 없음) → 질의 없이 `false` | 단위 (checker) |
| 삭제된 수업(`isDeleted`)은 겹침으로 보지 않음 | E2E |
| 다른 교사의 같은 시간 수업은 겹침 아님 | E2E |
| 제외 없음 / 수업 제외 / 과목 제외 | E2E |
| 1년 기간(52–53일) IN 목록 | E2E (실 PostgreSQL에서도) |
| 생성: 기간 시작·종료일이 그 요일일 때 포함, 시작>종료면 0건 | 단위 (generator) |
| 동기화: 같은 키 중복, 키 여러 개, 키 0개면 발행 안 함 | 단위 (collector) |

4. **실 DB 검증.** 테스트는 H2로 돈다. 운영은 PostgreSQL이라 `IN` 목록·`date`/`time` 비교가 다르게 동작할 수 있다.
   로컬 PostgreSQL 18 임시 인스턴스에 Flyway 마이그레이션을 적용하고 E2E(`*SubjectUpdateTest*`, `*LessonCreateTest*`,
   `*LessonUpdateTest*`, `e2e/daily_schedule/**`)를 한 번 더 돌린다 (방법: 메모리 `local-postgres-test`).
   판정: H2·PostgreSQL 둘 다 통과.
5. **배포 뒤 dev 확인.** 아래 «배포 뒤» 절차. 실패하면 PR을 되돌린다.

### 7. A — 요청 목록 N+1 — `fix(request)`

| 위치 | 지금 | 바꾼 뒤 |
|---|---|---|
| `application.yml` | `default_batch_fetch_size` 없음. 지연 연관을 행마다 1번씩 읽는다 | `spring.jpa.properties.hibernate.default_batch_fetch_size: 100`. 결석 요청 목록 `2 + 4P` → `2 + 4`, 교환 요청 목록 `2 + P` → `3` |
| `LessonExchangeRequestAdminViewService:57-62` | 대시보드 상위 10건마다 count 2번 (31 질의) | `request.id IN (...)`으로 상태별 개수를 한 번에 세는 질의 1개 (`group by request.id, status`) → 3 질의 |

전역 batch size는 이미 entity graph가 있는 조회에는 영향이 없고, 지연 로딩이 일어나는 곳만 묶는다.
판정: 결석·교환 요청 목록 E2E와 관리자 대시보드 E2E 통과, 질의 수 측정값을 `review.md`에 남긴다.

### 8. B — 스케줄러 정리 — `fix(request)`, `fix(file)`

| 작업 | 지금 | 바꾼 뒤 |
|---|---|---|
| 결석 요청 만료 (매분) | 만료 대상을 전부 읽어 행마다 UPDATE (1 + N) | 조건부 일괄 UPDATE 1번: `status = EXPIRED where status = PENDING and expires_at <= :now` (`version`·`updated_at`도 같이 올린다) |
| 교환 요청 만료 (매분) | 행마다 UPDATE + 제안 목록 지연 로딩 (1 + 2N + 제안 수) | 일괄 UPDATE 2번: 만료될 요청의 ACTIVE 제안을 CLOSED로 → 요청을 EXPIRED로 |
| 만료와 승인이 동시에 | 락·버전이 없어 나중 것이 덮어쓴다 | `AbsenceRequest`·`LessonExchangeRequest`에 `@Version`. 만료가 먼저 끝나면 승인 쪽 flush가 `ObjectOptimisticLockingFailureException` → 전역 처리기에서 409 `BIZ004 유효하지 않은 상태입니다` |
| 파일 정리 (매일 03:00) | 전체가 트랜잭션 1개, GCS 삭제가 트랜잭션 안. 하나 실패하면 DB는 전부 롤백되고 GCS는 이미 지워짐 | 클래스의 `@Transactional`을 떼고 청크마다 트랜잭션(`TransactionTemplate`). GCS 삭제는 트랜잭션 밖에서 먼저, DB 정리는 성공한 파일만. DB가 실패해도 다음 실행에서 GCS 삭제가 «없음 = 성공»으로 통과해 정리된다(`GcsStorageService.delete`는 없는 객체에 `true`) |

여러 서버 동시 실행(ShedLock)은 넣지 않는다. dev·운영 모두 앱 서버가 1대다.
`ponytail:` 주석으로 «서버가 2대 이상이 되면 ShedLock» 상한을 남긴다.

**Flyway `V9__add_request_version_columns.sql`**

```sql
ALTER TABLE absence_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE lesson_exchange_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

`init_scheme.sql`의 두 테이블에도 같은 컬럼을 넣는다 (`SqlSchemaConsistencyTest`).
기존 행은 0으로 채워진다. 컬럼 추가는 기본값 상수라 PostgreSQL에서 테이블 재작성 없이 끝난다.

판정:
- 단위: 만료 서비스가 일괄 UPDATE 결과 건수를 돌려준다, 파일 정리가 GCS 실패 파일은 DB를 남기고 다음 청크를 계속한다
- E2E: 만료 시각이 지난 요청이 스케줄러 메서드 호출 뒤 EXPIRED, 제안 CLOSED / 만료된 요청 승인 시 409 / 같은 요청을 두 번 저장(낡은 버전)하면 409
- PostgreSQL 임시 인스턴스에서 V9 적용과 위 E2E 통과

### 9. C — 커버리지 도구 — `chore(global)`

HeyMoa 서버처럼 하한을 두지 않는다. CI 댓글 같은 장치도 붙이지 않는다.
`build.gradle`에 `id 'jacoco'`만 더하고 `jacocoTestReport`가 XML·HTML을 만들게 한다(`test`가 `finalizedBy`).
리포트는 `build/reports/jacoco/test/html`. 판정: `./gradlew test` 뒤 리포트가 생긴다, `verify.sh` 통과.

### 10. 전체 검사

판정: `scripts/harness/verify.sh` 통과.

## 실패 경로

| 상황 | 처리 |
|---|---|
| 1분 이상 겹친다 | 지금과 같다. 과목 경로 409 `BIZ-05-002`, 수업 경로 `LessonDuplicateException` |
| 자동 생성 중 겹치는 날짜 | 지금과 같다. 그 날짜만 건너뛴다. 로그를 날짜 목록 한 줄로 남긴다 |
| 동기화 중 예외 | 지금과 같다. `BEFORE_COMMIT`이라 예외면 트랜잭션 전체가 롤백된다. 모아서 발행해도 같은 트랜잭션 안이다 |
| 동시에 같은 교사에게 겹치는 수업 저장 | 지금도 락이 없다. 이번 범위 밖 |
| 일부만 성공 (프론트 칸 저장) | 백엔드 요청 하나는 원자적이다. 칸 단위는 Frontend #211·#212 |

## 질의

| 누가 | 조건 | 행 수 | 빈도 |
|---|---|---|---|
| 과목 일정 재생성 검사 | 교사 + 날짜 IN(기간 안 그 요일) + 시간 | 교사의 해당 날짜 수업만 (한 달 4–5일 × 하루 3건 내외) | 관리자 저장마다. 질의 수: 날짜 수 → **1** |
| 시간만 변경·교사 배정 검사 | 교사 + 과목의 미래 수업 날짜 IN + 시간 | 〃 | 질의 수: 수업 수 → **1** (+ 수업 목록 조회 1) |
| 수업 자동 생성 | 교사 + 날짜 IN + 시간 | 〃 | 질의 수: 날짜 수 → **1** |
| DailySchedule 동기화 (과목 저장 1번) | (분반, 날짜)마다 수업·일정·교사 출석·학생 출석 | 키당 학생 출석 S건을 한 번에 | 질의 수: W × (8 + 2S) ≈ 600 → 키 수 × 5 + 신규 출석 insert ≈ 80 (W=16, S=15) |

판정용 측정: E2E에서 `spring.jpa.properties.hibernate.generate_statistics`로 과목 일정 재생성 1회의 질의 수를
수정 전·후로 세어 `review.md`에 남긴다.

`IN` 목록 크기: 과목 기간은 보통 한 달(4–5일), 길어도 1년(52–53일). DB 파라미터 상한에 한참 못 미친다.
기존 인덱스(교사 id·날짜)를 그대로 쓴다. 인덱스 추가 없음.

## 배포 뒤

dev에서 과목 59·78·160(3교시 13:00–13:30, 기간 7/1–8/31)을 10월 기간으로 다시 저장해 담당 교사 18·27·32의 10월 3교시 수업이 생기는지 확인한다.
