# 배포 전 테스트 실행과 로그에 요청 정보 남기기

- 이슈: #227
- 브랜치: `fix/227-deploy-test-and-exception-log`

## 무엇을 바꾸나

셋이다. 서로 기대지 않는다.

| 번호 | 무엇 | 파일 |
|---|---|---|
| 1 | 배포 전에 테스트를 돌리고, PR에서도 돌린다 | `deploy-dev.yml`, `deploy-prod.yml`, 새 `test.yml` |
| 2 | 모든 로그 줄에 요청(메서드·경로), 사용자 ID, 요청 ID를 남긴다 | 새 `RequestLogContextFilter`, `WebSecurityConfig` 한 줄, `logback-spring.xml` 한 줄 |
| 3 | 토큰 재발급이 「자격 증명 없음」만 401로 바꾸고 나머지는 그대로 올린다 | `LocalAuthService` |

### 1. 테스트

```yaml
# deploy-dev.yml, deploy-prod.yml
- run: ./gradlew bootJar -x test
+ run: ./gradlew test bootJar
```

테스트가 실패하면 빌드 단계에서 멈춰 SSH 배포 단계로 가지 않는다. PR에는 `./gradlew test`만
돌리는 `test.yml`을 둔다(`dev`·`main` 대상). 체크가 생겨야 `land-a-pr`의 「CI가 통과한 뒤에
병합한다」가 뜻을 가진다.

이슈의 「미정」(배포에서 돌릴지 PR에서만 돌릴지)은 **둘 다**로 정한다. PR 체크만 두면 브랜치
보호 없이 병합을 막지 못하고, 배포에만 두면 병합한 뒤에야 안다. 배포 시간은 테스트만큼(약 3분)
늘어난다.

### 2. 요청 정보

`GlobalExceptionHandler`의 로그 스무 줄을 하나씩 고치지 않는다. 요청이 들어올 때 MDC에 넣으면
**그 요청 동안 찍히는 모든 로그**에 붙는다.

```java
// RequestLogContextFilter — JwtAuthenticationFilter 바로 뒤에서 돈다
MDC.put("trace_id", UUID.randomUUID().toString());
MDC.put("request", request.getMethod() + " " + request.getRequestURI());
if (principal instanceof CustomUserDetails user) MDC.put("user_id", String.valueOf(user.getUserId()));
try { chain.doFilter(...); } finally { MDC.remove(...); }
```

- JWT 필터 뒤에 두어야 사용자 ID를 안다. `WebSecurityConfig`에 `addFilterAfter` 한 줄
- 로그 파일 패턴의 비어 있던 `trace_id` 자리를 채우고, 옆에 `request`, `user_id`를 더한다
- 콘솔(JSON)은 `LogstashEncoder`가 MDC를 그대로 싣는다
- 쿼리 문자열은 남기지 않는다(`getRequestURI`). 토큰이 쿼리에 실리는 경로가 있다
- 사용자 ID는 숫자 키만 남긴다. 이름·이메일은 남기지 않는다

### 3. 토큰 재발급

```java
- } catch (Exception e) {
+ } catch (CredentialNotFoundException e) {
```

`getById`가 자격 증명이 없을 때 던지는 것이 `CredentialNotFoundException`뿐이다. DB 오류 같은
나머지는 그대로 올라가 전역 처리기가 500과 스택트레이스로 남긴다.

## 실패 경로

| 상황 | 결과 |
|---|---|
| 배포 때 테스트 실패 | 빌드 단계에서 멈춘다. 서버는 이전 jar 그대로 |
| 인증 없이 들어온 요청의 오류 | `user_id` 없이 `request`, `trace_id`만 남는다 |
| 필터 전에 난 오류(보안 필터 자체) | 요청 정보가 안 붙는다. 드물고 이 이슈 범위 밖 |
| 자격 증명 조회 중 DB 오류 | 지금은 401, 바뀐 뒤는 500 + 스택트레이스 |

## 테스트 (먼저 쓰고 실패를 본다)

| 층 | 테스트 | 고치기 전 |
|---|---|---|
| 단위 | 재발급: 자격 증명이 없으면 `InvalidRefreshTokenException` | 통과 (지금도 그렇다. 회귀 방지) |
| 단위 | 재발급: 조회가 `DataAccessException`을 던지면 그대로 올라간다 | 실패 |
| 단위 | 필터: 체인이 도는 동안 MDC에 `request`·`trace_id`·`user_id`가 있고, 끝나면 지워진다 | 실패 (클래스 없음) |
| E2E | 권한 없는 API 호출의 경고 로그에 `request`와 `user_id`가 실린다 | 실패 |
| E2E | 로그인 없이 부른 오류 로그에는 `user_id` 없이 `request`만 실린다 | 실패 |
| 워크플로 | 이 PR에서 `test.yml`이 돌고 통과한다 | — |

「실패하는 테스트가 있으면 배포가 안 된다」는 워크플로를 한 번 일부러 깨뜨려 볼 방법이 이
PR 안에는 없다. `test` 뒤에 `bootJar`가 오는 한 줄이라 Gradle 동작에 기댄다.

## 작업

1. 테스트 넷 → 실패 확인 → 커밋 `test(global): …`
2. 필터 · 보안 설정 · 로그 패턴 → 커밋 `feat(global): 로그에 요청 정보 …`
3. 재발급 예외 → 커밋 `fix(auth): …`
4. 워크플로 셋 → 커밋 `ci(deploy): …`
5. PR을 열어 `test.yml` 체크가 도는지 본다

## 안 하는 것

- 브랜치 보호 규칙 설정(GitHub 설정. 사람이 켠다)
- 테스트 시간 단축, 분산 추적 도구
- 접근 거부 · Refresh Token 오류의 원인 분석(이 PR 뒤에 로그를 보고 한다)
- `span_id` 채우기
