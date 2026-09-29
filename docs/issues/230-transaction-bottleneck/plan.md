# 메일 시간 제한과 조회수 한 문장 증가

- 이슈: #230
- 브랜치: `fix/230-transaction-bottleneck`

## 무엇이 실제로 문제인가 (2026-09-29 dev 서버)

`open-in-view`는 꺼져 있다. 트랜잭션이 끝나면 DB 연결이 돌아간다.

| 이슈 항목 | dev 실측 | 판단 |
|---|---|---|
| 메일 시간 제한 없음 | `MAIL_ENABLED=true`, 실제 SMTP, 로그 대체 꺼짐. 인증 메일 재발송은 트랜잭션 안에서 보낸다 | **고친다.** SMTP가 멈추면 요청 스레드와 DB 연결이 끝없이 묶인다 |
| 조회수 읽고 더해 저장 | 게시글 31개, 조회수 합 1,825. 회의록 8개 | **고친다.** 동시 조회 시 증가분이 사라지고, 조회만으로 `updated_at`이 바뀐다 |
| 결의서 생성 중 다운로드 | 구입 요청 19건, 영수증 8장 | 안 한다. 드물게 불리고 한 번에 몇 장이다 |
| 업로드 · 파일 정리 스케줄러 | 파일 15개 | 안 한다 |
| Push 발송 | 구독 0건 | 안 한다 |
| 과목 생성 반복 쿼리 | 과목 163개 중 160개가 7월 초기 입력, 8월 3개. 분반당 학생 최대 5명 | 안 한다. 학기 초 관리자 작업이다 |
| 구입 요청 상세의 거래처 전체 조회 | 거래처 4개 | 안 한다 |

## 무엇을 바꾸나

### 1. 메일 시간 제한

`application.yml`의 `spring.mail.properties.mail.smtp`에 셋을 더한다. 값은 환경 변수로 바꿀 수 있다.

```yaml
connectiontimeout: ${MAIL_SMTP_CONNECTION_TIMEOUT_MS:5000}
timeout: ${MAIL_SMTP_TIMEOUT_MS:10000}
writetimeout: ${MAIL_SMTP_WRITE_TIMEOUT_MS:10000}
```

연결 5초, 읽기 · 쓰기 10초. 정상 발송은 1초 안쪽이다. 시간이 넘으면 `JavaMailSender`가 예외를 던지고
`TemplateMailSenderService`가 이미 잡아 경고 로그를 남긴다(바꾸지 않는다).

### 2. 조회수

게시글(`Post`)과 회의록(`MeetingRecord`) 둘 다 같은 모양으로 바꾼다.

```java
// Repository
@Modifying
@Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
void incrementViewCount(@Param("id") Long id);

// Entity
@Column(name = "view_count", nullable = false, updatable = false)  // 증가는 위 쿼리로만
private long viewCount;
```

서비스는 권한 확인 뒤 `repository.incrementViewCount(id)`를 부르고, 응답에 보일 값을 위해
엔티티의 `incrementViewCount()`(메모리 값만 +1)를 그대로 부른다. `updatable = false`라
Hibernate는 이 필드 변경으로 UPDATE를 만들지 않는다. 그래서 글 전체 저장과 `updated_at` 변경이
없어진다.

UPDATE 문은 파생 쿼리로 만들 수 없어 `@Query`를 쓴다. 레포에 같은 모양이 이미 있다
(`PushSubscriptionRepository`, `PostAttachmentRepository`).

## 실패 경로

| 상황 | 결과 |
|---|---|
| SMTP가 연결은 받고 응답하지 않는다 | 10초 뒤 예외 → 경고 로그, 발송 결과 실패. 트랜잭션은 그만큼만 잡힌다 |
| SMTP 주소로 연결이 안 된다 | 5초 뒤 예외 |
| 같은 글을 동시에 N번 연다 | 조회수가 정확히 N 오른다 |
| 삭제된 글 · 권한 없는 글 | 지금처럼 404 · 403. 조회수는 오르지 않는다 (확인 뒤 증가) |
| 응답의 조회수 | 이번 조회를 더한 값. 동시에 다른 조회가 있었으면 그만큼은 응답에 안 보인다 (DB 값은 정확) |

## 테스트 (먼저 쓰고 실패를 본다)

| 층 | 테스트 | 고치기 전 |
|---|---|---|
| 단위 | 연결만 받고 말이 없는 SMTP 소켓에 `application.yml` 설정 그대로 만든 `JavaMailSender`로 보내면 정해진 시간 안에 예외 | 실패 (끝나지 않음) |
| E2E | 같은 게시글을 동시에 10번 열면 조회수가 10 오른다 | 실패 예상 |
| E2E | 게시글을 열어도 `updated_at`이 그대로다 | 실패 |
| E2E | 회의록을 열어도 `updated_at`이 그대로이고 조회수가 1 오른다 | 실패 |
| 회귀 | 기존 `PostBoardQueryTest` · `MeetingRecordApiTest`의 조회수 응답 | — |

## 작업

1. 테스트 → 실패 확인 → 커밋 `test(global): …`
2. 메일 시간 제한 → 커밋 `fix(global): …`
3. 게시글 · 회의록 조회수 → 커밋 `fix(post): …`
4. 이슈 본문 범위를 위 표에 맞게 고친다

## 안 하는 것

위 표의 「안 한다」 다섯 줄. 행 수가 늘면 새 이슈로 올린다. 메일을 커밋 뒤로 옮기는 것도
하지 않는다 — 시간 제한으로 묶이는 시간의 상한이 생긴다.
