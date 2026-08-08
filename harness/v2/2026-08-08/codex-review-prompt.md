`dev` 브랜치 대비 이 브랜치의 커밋 변경(diff)을 리뷰해 주세요. 이 레포는 금정야학
백엔드(GJLearn API)입니다. Java 21 · Spring Boot 3.5 · Spring Data JPA · Gradle,
도메인 기반 레이어드 아키텍처입니다.

리뷰 기준의 원본은 `CLAUDE.md`와 `docs/convention/convention.md`입니다. 먼저 읽어
주세요. 아래는 그중 이 리뷰에서 특히 자주 오탐이 나는 자리를 다시 적은 것입니다.

## 이 레포에서 특히 봐야 하는 것

**도메인 경계.** `domain/{auth,users,classroom,student,subject,lesson,request,...}`
사이에는 통로가 둘 있고, 무엇을 하느냐로 갈립니다.

- **조회는 `*ProxyService`로** — 도메인이 좁은 읽기 전용 API를 내놓고 다른 도메인이
  그것을 주입합니다 (`UserProxyService`, `SubjectProxyService` 등 15개).
  **다른 도메인의 ProxyService를 주입한 것은 정상입니다. 지적하지 마세요**
- **상태 변화·부수 효과는 이벤트로** — Spring ApplicationEvent (이벤트 23개, 리스너
  15개). `SubjectCreatedEvent` → 수업 생성처럼 도메인을 건너뛰는 결과가 여기 옵니다

지적할 것은 **이 둘을 우회한 자리**입니다 — 다른 도메인의 Repository를 직접 주입하거나,
Proxy가 아닌 내부 Service를 가져다 쓰거나, 부수 효과를 이벤트 대신 직접 호출로 처리한
경우입니다.

**이벤트를 «불필요한 간접 호출»로, ProxyService를 «불필요한 래퍼»로 지적하지
마세요.** 둘 다 이 레포가 의도한 구조입니다.

**레이어 방향.** `controller → service → repository`. Controller가 Repository를 직접
부르거나 **Entity를 그대로 응답에 싣는 것**은 지적 대상입니다. 응답은 도메인별
`*Response` record로 나갑니다.

컨트롤러는 `ResponseEntity<XxxResponse>`(본문 없으면 `ResponseEntity<Void>`)를
반환합니다. **`ApiResponse` 같은 공통 래퍼는 이 레포에 없습니다.** 래퍼로 감싸라는
지적은 반려됩니다. 에러는 `common/advice`의 전역 예외 처리기가 도메인별 `*ErrorCode`를
받아 변환합니다.

**엔티티.** 엔티티는 `BaseEntity`를 상속합니다(`createdAt`·`updatedAt` 자동). 새
엔티티가 그것을 안 했으면 지적해 주세요.

**단, 기존 예외 넷은 지적 대상이 아닙니다** — `RefreshToken`, `File`,
`PurchaseRequestItem`, `PurchaseRequestPaymentTransaction`. 이번 변경이 만든 것이
아닙니다(@Entity 45개 중 넷, 2026-08-08 기준).

**스키마.** 엔티티가 바뀌면 Flyway 마이그레이션(`src/main/resources/db/migration/`)과
`src/main/resources/sql/init_scheme.sql`이 같이 바뀌어야 합니다. 한쪽만 바뀐 것은
`SqlSchemaConsistencyTest`가 잡지만, 마이그레이션 번호가 겹치는 것은 못 잡습니다.

**권한.** 새 엔드포인트는 `@PreAuthorize`를 답니다. 층이 둘입니다.

- **역할** — `RoleType`은 `ADMIN` · `MANAGER` · `VOLUNTEER` · `GUEST` 넷뿐이고
  `ROLE_` 접두사가 붙습니다. `hasRole('ADMIN')`으로 씁니다
- **세밀 권한** — `PermissionCode` 값 객체이고 형태는 `resource:action:target`입니다.
  `hasAuthority('user:manage:*')`처럼 문자열로 씁니다.
  `resource`는 `ResourceType`(channel · subject · student · department · lesson ·
  daily_schedule · user · absence_request · purchase_request · vendor · event ·
  teacher_application · lesson_exchange_request),
  `action`은 `ActionType`(read · write · grant · manage · review),
  `target`은 `*`(global) 또는 양의 정수 ID입니다.
  등록되지 않은 조합은 `PermissionRegistry.validate`가 거절합니다

`hasAuthority('TEACHER')`나 `hasAuthority('DEPT_FINANCE')`처럼 **역할 이름을
`hasAuthority`에 넣는 표기는 이 레포에 없습니다.** 그 표기를 쓰라고 지적하지 마세요.

**N+1.** 목록 조회에서 연관을 건건이 부르는 코드를 봐 주세요. 이 레포에서 반복해서
나온 결함입니다.

**테스트.** 테스트는 세 층입니다 — `src/test/java/geumjeongyahak/`의 `e2e/`(요청부터
응답까지, 권한 포함), `unit/`(서비스·엔티티·검증기의 판단), `domain/`(외부 연동을
낀 서비스와 변환기). 새 엔드포인트에 E2E가 없거나, 권한 분기가 있는데 통과하는
역할만 치고 막히는 역할을 안 친 경우를 지적해 주세요. 날짜를 고정값으로 박은
테스트도 지적 대상입니다 — 그날이 지나면 깨집니다.

빌드는 Gradle입니다(`./gradlew test`). 검사 묶음은 `scripts/harness/verify.sh`
하나입니다. **그 밖의 린터·포매터·타입체커를 새로 도입하라는 지적은 반려됩니다.**

## 지적하기 전에

**재현 경로를 대세요.** 어떤 입력·상태에서 무엇이 되는지 구체적으로 못 적으면 그
지적은 반려됩니다. "~할 수 있다"는 추측이고, 추측은 `speculative`로 표시해 주세요.

**지금 동작을 먼저 읽으세요.** 이미 그렇게 도는 것을 고치라는 지적이 자주 나옵니다.
코드를 읽고, 그래도 다르면 그때 지적하세요.

**이번 변경이 만든 것만 봅니다.** 원래 있던 결함, CI·의존성·모듈을 새로 만들라는
요구, 다른 도메인까지 리팩터하라는 것은 이 PR이 할 일이 아닙니다.

**문체는 지적 대상이 아닙니다.** `docs/`의 설명 산문, 주석의 평어·존댓말은 위반이
아닙니다.

## 출력 형식

찾은 것을 **한 번에 전부** 열거해 주세요. 나눠서 내면 회차가 늘어납니다.

지적마다 심각도와 위험도를 답니다.

- 심각도 — `P1`(병합을 막는다) · `P2`(고쳐야 한다) · `P3`(알아두면 좋다)
- 위험도 — `blast`(넓게 번진다) · `blocking`(막힌다) · `local`(그 자리뿐) ·
  `speculative`(확인 안 됨)

```
- [P1][blast] 요약 — file:line
  무엇 / 어떻게 / 왜
```

지적할 것이 없으면 **`P1·P2 없음`이라고 정확히 적어 주세요.** 아무것도 안 적으면
출력이 깨진 것인지 없는 것인지 구별이 안 됩니다.

날카롭게 보되 근거를 대세요. 근거 없는 지적은 세지 않습니다.
