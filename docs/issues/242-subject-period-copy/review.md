# #242 검토 기록

## 규칙 1차 검증 (codex 전, work-an-issue 35)

| 문서 · 항목 | 판정 | 근거 |
|---|---|---|
| CLAUDE.md · 도메인 경계 | 지킴 | subject → lesson은 `SubjectsCopiedEvent`(신규), 교사 수업 겹침은 `LessonProxyService`. 새 Repository 주입은 `SubjectCopyService`의 `SubjectRepository`(자기 도메인)뿐 |
| CLAUDE.md · 응답 record · ErrorCode | 지킴 | `SubjectCopyResponse`·`SubjectCopyFailure` record, `SubjectErrorCode`에 `BIZ-05-003`·`VAL-05-002` |
| CLAUDE.md · 권한 | 지킴 | `@PreAuthorize(SUBJECT_WRITE_ACCESS)` — 과목 생성과 같음. E2E: `subject:write:*` 201, 봉사자 403 |
| writing-a-plan · 실패 경로·질의 | 지킴 | 실패 수집·저장 없음·두 번 보내기·동시 복사 한계·교사 없는 과목. 질의는 과목 수에 비례(검증), 동기화는 (분반, 날짜)마다 1번 |
| writing-a-plan · 바뀐 것 | 지킴 | 원본 지정 `sourceDate` → `subjectIds`(사용자 결정), 보낸 과목끼리 충돌 검사 추가 |
| changing-code · 호출자 | 지킴 | 전역 처리기 변경은 `ProblemDetailProperties` 구현체만 영향(1개). `SubjectScheduleValidator`를 public으로 — 기존 호출은 같은 패키지 `SubjectService`뿐 |
| changing-code · 공유 지점 | 지킴 | 판정은 과목 생성의 `SubjectScheduleValidator`를 그대로 씀. 수업 생성은 기존 `LessonGenerator` |
| changing-code · 필요 없는 것 | 지킴 | 미리보기 API·부분 칸 지정·동시 잠금은 «안 하는 것» |
| testing · 고치기 전 실패 | 지킴 | E2E 9건이 엔드포인트 없을 때 실패, 묶음 충돌 1건은 구현 중 추가 |
| testing · 목 되읽기 | 지킴 | 단위는 «검증기가 던진 사유가 failures에 실리고 저장이 없는가»를 본다 |
| testing · 권한 통과·거절 | 지킴 | 위 권한 |
| testing · 날짜 미래 | 지킴 | 2099년 학기 |
| diff-signals | 해당 없음 | 인자 없는 `now()`·원인 버리는 `catch`·트랜잭션 안 외부 호출 없음. 반복문 안 질의는 과목마다 검증(월 1회 관리자 작업, 계획에 기록) |

## 질의·동기화 측정

`SubjectCopyQueryCountTest` (H2): 한 분반 1~3교시 복사 1회 — DailySchedule 동기화 **4번**(4월 월요일 수). 과목마다 생성 이벤트를 냈다면 12번.
문 73개(쓰기 31, 조회 42).

## 커버리지 (JaCoCo, `verify.sh` 전체 1197건 뒤)

| 클래스 | 줄 | 분기 |
|---|---|---|
| `SubjectCopyService` | 98/98 (100%) | 20/22 (90%) |
| `SubjectCopyConflictException` | 4/4 (100%) | - |
| `SubjectCopyFailure` | 8/8 (100%) | - |
| `SubjectsCopiedEvent` | 5/5 (100%) | - |
| `SubjectLessonScheduleService` | 43/43 (100%) | 2/2 (100%) |
| `SubjectEventHandler` | 57/57 (100%) | - |

## 실제 PostgreSQL 18 검증

Flyway V1–V9 적용 후 `SubjectCopyTest`·`SubjectScheduleUpdateTest`·`SubjectTeacherAssignTest`·`SubjectCreateTest` 51건 중 50건 통과.
실패 1건(`SubjectTeacherAssignTest` 교사 해제)은 `users` 기본키 100 충돌 — 다른 테스트가 id를 직접 넣은 뒤 시퀀스가 그 값에 닿는 순서 의존이다.
새 DB에서 그 클래스만 돌리면 9건 모두 통과. #242 변경과 무관한 기존 테스트 격리 문제라 기록만 한다.

## codex 리뷰

| 회차 | 범위 | P1 | P2 | 수용 | 반려 | 남은 P1·P2 |
|---|---|---|---|---|---|---|
| 1 | `dev` 대비 전체 | 1 | 2 | 3 (보낸 과목끼리 교사 하루치 일정, 같은 칸 모든 쌍, 새 기간 요일 유무) | 0 | 0 |
| 2 | `dev` 대비 전체 | 0 | 1 | 0 | 1 (생성 API와 #199 판정 차이 — 복사는 생성+배정, 생성 API의 빈틈은 범위 밖) | 0 |
| 확인 | `dev` 대비 전체 | 0 | 0 | — | — | **0** |

지적 원문과 사유는 `harness/findings-ledger.md` 2026-10-01 #242 행.
