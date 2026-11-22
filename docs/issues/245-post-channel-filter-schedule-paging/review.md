# #245 검토 기록

## 구현 중 확인한 것

| 계획의 열린 점 | 확인 결과 |
|---|---|
| 공백만 있는 일지(Java `isBlank` vs SQL `trim`) | 차이 없음. `note`를 쓰는 곳은 `DailyScheduleService`의 일지 저장 하나이고, 요청의 `LessonJournalRequest.note`가 `@NotBlank`라 공백만 있는 일지는 저장되지 않는다 |
| 키워드 `%`·`_` | `LIKE ... ESCAPE '\'`로 글자 그대로. E2E로 확인 |

## diff 신호 (rules/diff-signals)

| 신호 | 판정 |
|---|---|
| `findAll` 뒤 `stream().filter`·`subList` | 이번 변경으로 **없어짐** (`getJournalDailySchedules`) |
| 반복문 안 질의 · 트랜잭션 안 외부 호출 · 인자 없는 `now()` · 원인 버리는 catch · 락 없는 확인 후 변경 | 추가된 줄에 없음 |

## 커버리지 (JaCoCo, 바뀐 테스트만 돌린 PostgreSQL 회차)

| 대상 | 줄 | 분기 |
|---|---|---|
| `DailyScheduleSpecs` | 20/20 | - |
| `DailyScheduleService.getJournalDailySchedules` | 12/12 | 3/4 |
| `PostSearchSpecificationBuilder` | 25/32 | 21/38 |
| `PostSpecs` | 15/28 | - |

게시글 두 클래스의 못 덮은 줄은 이번에 안 바꾼 기존 필터(작성자·본문·고정 등)로, 좁은 회차에서 그 테스트를 안 돌렸기 때문이다.

## 검사

- `scripts/harness/verify.sh` 통과 (H2, 1191건)
- 임시 PostgreSQL 18.4: Flyway V1–V9 적용 후 `DailyScheduleJournalListTest`(4) · `DailyScheduleReadTest`(11) · `PostBoardQueryTest`(8) 통과
  (`stringtype=unspecified`, 테스트 실행에만)

## codex 리뷰

| 회차 | 범위 | P1 | P2 | 수용 | 반려 | 남은 P1·P2 |
|---|---|---|---|---|---|---|
| 1 | `origin/dev`(fec8c012) 대비 전체 | 0 | 0 | — | — | **0** |
