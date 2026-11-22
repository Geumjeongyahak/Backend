# 게시글 검색에 다중 채널 유형 포함·제외 필터를 더하고, 수업일지 목록의 필터·페이징을 쿼리로 옮긴다

이슈: #245 [FEAT] 게시글 다중 채널 필터와 수업일지 목록 DB 페이징
소비자: Frontend #214 (게시판 «전체» 탭·관리자 대시보드의 전체 조회와 100건 잘림 제거)

## 1. 게시글 다중 채널 유형 필터

`GET /posts`(`PostBoardController` → `PostCrudService.getPosts` → `PostSearchSpecificationBuilder.build(PostBoardSearchRequest)`)는
`channelType` 하나만 받는다. 프론트는 «NOTICE·CLASSROOM·DEPARTMENT만»을 원해서 전부 받아 거른다
(`AdminPostsSection.tsx:280`, `AdminDashboardPage.tsx:703` `useClientPostFiltering`).

`PostBoardSearchRequest`에 둘을 더한다.

| 파라미터 | 타입 | 뜻 |
|---|---|---|
| `channelTypes` | `List<ChannelType>` | 이 유형들 중 하나인 채널의 글만 (`IN`) |
| `excludedChannelTypes` | `List<ChannelType>` | 이 유형들의 채널 글은 빼기 (`NOT IN`) |

- 쿼리스트링은 `channelTypes=NOTICE,CLASSROOM` 또는 `channelTypes=NOTICE&channelTypes=CLASSROOM` 둘 다 받는다(Spring 기본 바인딩)
- 모든 조건은 AND로 붙는다. 기존 `channelType`·`channelId`·`classroomId`·`departmentId`는 그대로 둔다 — 응답 형식·기존 파라미터 동작이 안 바뀐다
- 빈 목록이나 생략은 필터 없음
- 잘못된 값(`channelTypes=FOO`)은 `BindException` → 전역 처리기의 400 (`GlobalExceptionHandler` `@ExceptionHandler(BindException.class)`)
- `PostSpecs`에 `hasChannelTypeIn(Collection<ChannelType>)`·`hasChannelTypeNotIn(...)` 두 개를 더한다
- 권한 변경 없음 — 읽을 수 있는 채널 조건(`readableBoardChannelSpec`)은 그대로 AND로 붙는다

건드리는 파일: `PostBoardSearchRequest`, `PostSearchSpecificationBuilder`, `PostSpecs`.
PR #247이 바꾸는 `PostRepository`·`BasePaginationRequest`·`SortOrders`는 건드리지 않는다.

질의:

| 누가 | 조건 | 읽는 행 | 빈도 |
|---|---|---|---|
| 게시판 «전체» 탭, 관리자 대시보드 | `channel.channel_type IN (...)` + 기존 조건, 페이지 크기만큼 | 페이지 크기(기본 10, 최대 100) + count 1 | 화면을 열 때마다 |

`posts`는 수백~수천 행이고 채널 조인은 이미 있다(`channel.isDeleted`). 인덱스는 더하지 않는다.

판정: E2E `e2e/post/PostBoardQueryTest`
① `channelTypes=NOTICE,CLASSROOM`이면 그 두 유형 글만, `totalElements`가 서버 기준으로 맞다
② `excludedChannelTypes=EVENT`이면 EVENT 글이 없다
③ `channelTypes=FOO`는 400
④ 반복 파라미터 형식(`channelTypes=NOTICE&channelTypes=CLASSROOM`)도 같은 결과

## 2. 수업일지 목록 DB 페이징

`DailyScheduleService.getJournalDailySchedules`(188–226줄)는 삭제 안 된 하루 일정을 **전부** 읽고, 그 날짜·분반의 수업을 **전부** 읽은 뒤
Java에서 «내 것» · «일지 작성됨» · 키워드로 거르고 `subList`로 자른다. 요청 하나가 테이블 전체에 비례한다.

### 설계

`DailyScheduleRepository`를 `JpaSpecificationExecutor<DailySchedule>`로 만들고 `DailyScheduleSpecs`(신규, `daily_schedule/repository/`)에
지금의 Java 필터를 그대로 옮긴다.

| 지금 (Java) | 쿼리 |
|---|---|
| `isDeleted = false` | `ds.isDeleted = false` |
| `mine` → `teacher.id == requesterId` | `ds.teacher.id = :requesterId` (`mine=true`일 때만) |
| `hasWrittenJournal`: 그 분반·날짜의 삭제 안 된 수업 중 `note`가 비지 않은 것이 하나라도 | `EXISTS (Lesson l: l.isDeleted=false, l.subject.classroom = ds.classroom, l.date = ds.lessonDate, trim(l.note) <> '')` |
| `matchesKeyword`: 분반명 · 교사명 · (그 수업들의) 과목명 · 일지 내용, 대소문자 무시 부분 일치 | `lower(classroom.name) LIKE` OR `lower(teacher.name) LIKE` OR `EXISTS (같은 수업 조건 + lower(subject.name) LIKE OR lower(note) LIKE)` |
| 정렬 `lessonDate DESC, id DESC` | 같은 정렬 |

- 페이지에 든 일정만 기존 `getLessonsByScheduleKey`(→ `LessonProxyService.getActiveLessonsByClassroomIdsAndDates`)와
  `getTeacherAttendancesByScheduleId`로 채운다. 응답 조립(`toSummaryResponse`)은 그대로 — **응답 형식 불변**
- 키워드의 `%`·`_`·`\`는 이스케이프한다. 지금 Java `contains`는 글자 그대로 찾으므로 그 동작을 지킨다
- `@EntityGraph(classroom, teacher)`는 `findAll(Specification, Pageable)` 오버라이드로 유지

**도메인 경계.** 서브쿼리가 `Lesson` 엔티티를 JPQL/Criteria에서 참조한다. 다른 도메인의 Repository·Service를 주입하지 않는다.
같은 형태의 선례가 있다 — `AbsenceRequestRepository.existsByDailyScheduleMatchingLessonIds`(request → `Lesson` EXISTS),
`FileRepository.findUnlinkedPurchaseItemFilesBefore`. 페이징을 DB에서 하려면 필터가 한 쿼리 안에 있어야 해서 Proxy 두 번 호출로는 못 한다.

**공백 판정 차이.** Java `isBlank`는 모든 공백 문자를, SQL `trim`은 스페이스만 걷는다. 줄바꿈·탭만 있는 일지는 지금은 «안 씀», 바뀐 뒤엔 «씀»이 된다.
일지 저장 경로(`createJournal`·`updateJournal`)가 빈 일지를 막는지 구현 때 확인하고, 막지 않으면 이 차이를 PR `리뷰어에게`에 적는다.

### 질의

| 누가 | 조건 | 읽는 행 | 빈도 |
|---|---|---|---|
| 교사·관리자 수업일지 목록 | 위 표 | 페이지 크기(최대 100) + count 1 + 그 페이지의 수업 1 + 교사 출석 1 = **4질의** | 화면을 열 때마다 |

지금은 `daily_schedules` 전체 + 그 전체에 걸친 `lessons` + 출석 1. `daily_schedules`는 분반 수 × 수업일만큼 매일 늘고
(분반 ~10 × 연 ~200일 ≈ 연 2,000행), `lessons`는 그 몇 배다.

EXISTS 서브쿼리는 `lessons.date`(`idx_lessons_date`)로 좁혀진 뒤 `subject` 조인으로 분반을 맞춘다. 새 인덱스는 더하지 않는다 —
연 수천 행 규모이고, 필요해지면 `lessons(date, subject_id)` 복합 인덱스를 이 질의와 함께 따로 낸다.

### 실패 경로

| 상황 | 처리 |
|---|---|
| 목록 조회 중 다른 요청이 일지를 쓴다 | 읽기 전용 트랜잭션 두 질의(목록·count) 사이에 행이 바뀌면 `totalElements`가 한 건 어긋날 수 있다. 지금 Java 방식도 같다. 막지 않는다 |
| 페이지가 범위를 넘는다 | 빈 `content`, `totalElements`는 그대로 (지금과 같음) |
| `keyword` 공백만 | 필터 없음 (지금 `hasText`와 같음) |

락·재시도·외부 호출 없음.

### 판정

- 단위 `unit/daily_schedule/DailyScheduleServiceReadTest`: 리포지토리 목을 `findAll(Specification, Pageable)`로 바꿔 기존 테스트를 맞춘다
- E2E `e2e/daily_schedule`(신규 `DailyScheduleJournalListTest`, 날짜는 미래 기준):
  ① 일지 없는 일정은 안 나온다 ② `mine=true`면 내 일정만 ③ 키워드가 분반명·교사명·과목명·일지 내용 각각으로 걸린다
  ④ `size=2`로 5건이면 `totalElements=5`, 2쪽에 2건, 3쪽에 1건, 정렬은 날짜·id 내림차순 ⑤ 키워드 `%`가 모든 일지에 걸리지 않는다
- 고치기 전에 ①–⑤를 돌려 기존 Java 구현에서도 통과하는지 본다 — **동작 보존 테스트**다. ⑤만 고친 뒤에도 같아야 한다
- 질의 수는 Hibernate 통계(`hibernate.generate_statistics`) 대신 E2E에서 확인하지 않는다 — 전체 적재가 사라진 것은 코드로 보인다

## 공통

- 스키마·Flyway 없음. `@PreAuthorize` 변경 없음
- 커밋: `test(post)` → `feat(post)`, `test(daily-schedule)` → `refactor(daily-schedule)`, `docs(daily-schedule)`
- 문서: `docs/api-spec`의 게시글 목록 파라미터에 두 필드 추가(해당 절이 있으면)
- 검증: `verify.sh`(H2) + 바뀐 E2E를 임시 PostgreSQL 18에서 한 번 더 (`lower`·`trim`·`LIKE ESCAPE`가 PG에서 같게 도는지)
- 마지막에 codex 리뷰 (두 도메인, 질의 구조 변경)

## 안 하는 것

- `channelType`(단수)의 잘못된 값이 `ChannelType.valueOf`에서 500이 나는지 — 이번 범위 밖. 구현 중 확인만 하고 그렇다면 별 이슈
- 프론트 변경 — Frontend #214
- `lessons` 복합 인덱스 — 위 «질의» 참고
