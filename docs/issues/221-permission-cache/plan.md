# 인증 요청의 사용자·권한 조회를 Caffeine 캐시에 올리고, 엔티티가 바뀌면 커밋 뒤 지운다

이슈: #221 `[FEAT] 인증 요청 권한 조회 인메모리 캐시 도입`

## 선행 — PR #243(#240) 병합 뒤 시작한다

#243이 이 계획과 같은 파일 셋을 고친다. 줄은 안 겹치지만 측정 기준이 바뀐다.

| 파일 | #243 | #221 |
|---|---|---|
| `build.gradle` | `jacoco` 플러그인·리포트 | cache · caffeine 의존성 |
| `domain/users/entity/User.java` | 끝에 `validateCanTeach()` | 클래스에 `@EntityListeners` |
| `application.yml` | `hibernate.default_batch_fetch_size: 100` | `spring.cache` 블록 |

`default_batch_fetch_size`는 캐시 miss 때 인증 쿼리 수에 닿는다. 작업 1의 «도입 전»
측정은 #243이 들어간 `dev`에서 해야 «도입 후»와 비교가 된다.

1. #243 병합 확인 (`gh pr view 243 --json state` → `MERGED`)
2. `git fetch origin && git rebase origin/dev` — `gates-ledger.tsv` 충돌은 두 행 다 남긴다
3. `scripts/harness/verify.sh`로 기준선을 다시 잡는다 (2026-10-01 첫 기준선은 rebase 전 `21d1b7d2`에서 통과)
4. 위 세 파일을 다시 읽고 작업 1부터

## 요지

- `CustomUserDetailsService.loadUserByUserId`에 `@Cacheable("userDetails")` 하나를 단다.
  JWT 필터만 이 메서드를 부른다 (`JwtAuthenticationFilter:35`). 로그인 경로의
  `loadUserByUsername`은 손대지 않는다.
- 캐시 설정은 **`application.yml`만으로** 한다 (`spring.cache.type=caffeine`,
  `cache-names`, `caffeine.spec`). 캐시가 하나라 `CacheConfig` 클래스가 필요 없다.
- 무효화는 서비스 메서드마다 `@CacheEvict`를 다는 대신 **JPA 엔티티 리스너 한 곳**에서
  한다. 이유는 아래 「무효화 지점」.

## 무효화 지점 — 왜 엔티티 리스너인가

`CustomUserDetails`가 담는 것은 역할 · 개인 권한 · 부서 권한 · 부서 id · 자격증명
(id · 이메일 · 비밀번호 해시) · 삭제 여부다. 이 값을 바꾸는 코드는 지금 열두 곳이다.

| 바뀌는 것 | 자리 |
|---|---|
| 역할 | `UserCrudService:360` `setRole` · `:352` `releaseTeacherProfile` · `:358` `approveTeacherProfile` · `TeacherApplicationEventHandler:22` · `AdminBootstrapRunner:89` · `LocalAuthService:104` `reactivateForSignup` |
| 부서 | `UserCrudService:226` · `:280` `setDepartment` |
| 개인 권한 | `UserPermissionService:38` 추가 · `:58` 전체 삭제 · `UserCrudService:354` `clearPermissions` · `TeacherAssignmentPermissionService:28` |
| 삭제 | `UserCrudService:410` `softDelete` |
| 자격증명 | `UserCredentialService:165,181` · `PasswordResetService:114` · `LocalAuthService:109` |
| 부서 권한 프리셋 | `DepartmentPermissionService` `replacePermissions` · `addPermission` (← `DepartmentCrudService:43,81`, `DepartmentAdminViewService:96`, `DepartmentProvisioningHandler`) |

전부 JPA 엔티티를 고치고 flush하는 길이다. 벌크 JPQL·네이티브 UPDATE/DELETE로 이
테이블들을 고치는 코드는 없다 (`grep -rni "update users\|delete from user\|jdbcTemplate"`
결과 0건). 파생 삭제(`deleteAllByUserId`, `deleteAllByDepartmentId`)도 엔티티를 읽어
`remove`하므로 `@PostRemove`가 돈다.

그래서 **열두 곳에 evict를 복사하지 않고, 이 길들이 모두 지나는 엔티티 콜백 한 곳에
둔다.** 열셋째 지점이 생겨도 안 빠진다.

### `UserDetailsCacheEvictor` (새 파일, `common/security/service/`)

```java
/**
 * 사용자·권한 엔티티가 바뀌면 커밋 뒤 userDetails 캐시를 지운다.
 * 벌크 JPQL·네이티브 UPDATE/DELETE 는 엔티티 콜백을 우회하므로 여기서 안 지워진다 —
 * 그런 코드를 더하면 그 자리에서 직접 지운다.
 */
@Component
@RequiredArgsConstructor
public class UserDetailsCacheEvictor {
    public static final String CACHE = "userDetails";
    private final CacheManager cacheManager;

    @PostPersist @PostUpdate @PostRemove
    void onChange(Object entity) {
        switch (entity) {
            case User u -> afterCompletion(cache -> cache.evict(u.getId()));
            case UserPermission p -> afterCompletion(cache -> cache.evict(p.getUser().getId()));
            case UserCredential c -> afterCompletion(cache -> cache.evict(c.getUser().getId()));
            case DepartmentPermission ignored -> afterCompletion(Cache::clear);
            default -> { }
        }
    }
    // 트랜잭션 동기화가 켜져 있으면 afterCompletion 에, 아니면 즉시 실행
}
```

- `@EntityListeners(UserDetailsCacheEvictor.class)`를 `User` · `UserPermission` ·
  `UserCredential` · `DepartmentPermission`에 단다. `BaseEntity`의
  `AuditingEntityListener`는 상속으로 그대로 남는다.
- Spring Boot는 Hibernate에 `SpringBeanContainer`를 넣으므로 리스너가 빈으로 주입된다.
- **부서 권한 프리셋이 바뀌면 `userDetails` 전체를 비운다.** 그 부서 소속만 골라 지우려면
  users 도메인에 «부서 id → 사용자 id 목록» 조회를 더해야 한다. 프리셋 변경은 관리자
  화면에서 드물게 일어나고, 비우면 다음 요청에서 한 번씩 다시 채울 뿐이다.
  `ponytail:` 주석으로 남긴다 — 사용자가 수천 명이 되면 부서 소속만 지운다.
- 도메인 경계: `common/security`가 이미 `CustomUserDetailsService`에서 users · auth ·
  department 엔티티를 직접 import 한다. 새 의존 방향은 없다. 이벤트·Proxy는 쓰지 않는다.

### 왜 커밋 뒤에 지우나

`@PostUpdate`는 flush 때 돈다. 그 자리에서 바로 지우면, 커밋 전에 들어온 다른 요청이
**아직 커밋 안 된 옛 값**을 DB에서 읽어 캐시에 다시 넣는다. 커밋 뒤(`afterCompletion`)에
지우면 그 사이 채워진 옛 값도 같이 지워진다. 롤백 때도 지우지만 해가 없다.

## 실패 경로

| 상황 | 처리 |
|---|---|
| 같은 사용자를 두 요청이 동시에 처음 읽는다 | 둘 다 DB를 읽고 같은 값을 넣는다. 결과가 같아서 막지 않는다 (`sync=true` 안 씀) |
| 커밋 직전에 옛 값을 읽은 요청이 `afterCompletion` 뒤에 캐시에 쓴다 | 옛 권한이 **최대 TTL 5분** 남는다. 막지 않는다 — 막으려면 버전 스탬프가 필요하고, 창이 수 ms다. 「알려진 한계」에 적는다 |
| 삭제·없는 사용자 | `UsernameNotFoundException`은 캐시에 안 들어간다(Spring 캐시는 예외를 저장하지 않음). 지금처럼 매번 DB를 본다 |
| 다중 인스턴스 | 인스턴스마다 따로 논다. 이슈의 「알려진 한계」 그대로 — TTL 5분이 노출 상한 |
| 캐시 크기 초과 | Caffeine이 오래 안 쓴 항목부터 내보낸다. 다음 요청에서 다시 읽는다 |

락·재시도·외부 호출은 없다.

## 질의

| 누가 | 조건 | 행 | 빈도 |
|---|---|---|---|
| JWT 인증 (캐시 miss) | `user_credentials` by user_id · `users` by id · `user_permissions` by user_id · `department_permissions` by dept+role | 1 · 1 · 수 개 · 수 개 | 사용자당 5분에 한 번, 또는 그 사용자 변경 직후 |
| JWT 인증 (캐시 hit) | 없음 | 0 | 나머지 모든 인증 요청 |

스키마 변경 없음 → Flyway 마이그레이션 없음, `init_scheme.sql` 그대로.
새 엔드포인트 없음 → 권한(`@PreAuthorize`) 변경 없음.

## 작업

### 1. 측정 테스트 먼저 (test)

`src/test/java/geumjeongyahak/e2e/auth/UserDetailsCacheTest.java` (새 파일).
`StudentListQueryCountTest`처럼 Hibernate `Statistics`를 쓴다 (`application-test.yml`에
`generate_statistics: true`가 이미 있다).

- **인증 쿼리가 두 번째 요청부터 안 나간다**: 같은 토큰으로 사용자·권한 테이블을 안 읽는
  엔드포인트를 두 번 부르고, 두 번째 요청의 `UserCredential` · `DepartmentPermission`
  엔티티 로드 수와 `User.permissions` 컬렉션 fetch 수 증가가 0인지 본다.
  첫 번째와 두 번째의 `getPrepareStatementCount()` 차이를 로그로 남겨 PR에 옮긴다.
- 지금 코드에서 **실패하는 것을 본다.**

판정: `./gradlew test --tests '*UserDetailsCacheTest*'`가 빨간불.

### 2. 캐시 켜기 (feat)

- `build.gradle`: `spring-boot-starter-cache`, `com.github.ben-manes.caffeine:caffeine`
  (버전은 Boot BOM)
- `GeumjeongyahakApplication` 또는 기존 설정 클래스 하나에 `@EnableCaching`
- `application.yml`:
  ```yaml
  spring:
    cache:
      type: caffeine
      cache-names: userDetails
      caffeine:
        spec: maximumSize=1000,expireAfterWrite=5m,recordStats
  ```
  `cache-names`를 미리 적어야 Boot가 시작 시점에 Micrometer `cache.*` 지표를 붙인다.
- `CustomUserDetailsService.loadUserByUserId`에 `@Cacheable(UserDetailsCacheEvictor.CACHE)`
  (상수는 3에서 생기므로 이 커밋에서는 문자열 `"userDetails"`로 두고 3에서 바꾼다)

판정: 1의 테스트가 초록. `CustomUserDetailsServiceTest`(단위, 목)는 프록시를 안 거치므로 그대로 초록.

### 3. 무효화 테스트 먼저, 그다음 리스너 (test → feat)

`UserDetailsCacheTest`에 더한다. 각각 **같은 토큰으로 먼저 한 번 불러 캐시를 데운 뒤**
상태를 바꾸고 다시 부른다.

| 바꾸는 것 | 어떻게 (API) | 기대 |
|---|---|---|
| 개인 권한 회수 | `UserPermissionController` 삭제/교체 | 그 권한이 필요한 엔드포인트 200 → 403 |
| 역할 강등 | 관리자 사용자 수정 API로 MANAGER → GUEST | 관리자급 엔드포인트 200 → 403 |
| 부서 권한 프리셋 회수 | `DepartmentCrudService` 수정 경로(부서 수정 API) | 부서 권한으로 열리던 엔드포인트 200 → 403 |
| 사용자 삭제 | 관리자 삭제 API | 인증 필요 엔드포인트 200 → 401/403 (지금 동작과 같은 코드) |

구체 엔드포인트는 기존 E2E 헬퍼(`TestUserHelper`, `DepartmentBaseTest`)로 만들 수 있는
것에서 고른다.

순서: 2까지 적용한 상태에서 이 테스트를 돌려 **빨간불(캐시된 옛 권한으로 200)을 본 뒤**
`UserDetailsCacheEvictor`와 `@EntityListeners` 넷을 더한다.

판정: `./gradlew test --tests '*UserDetailsCacheTest*'` 초록.

### 4. 관측 확인 (test)

`UserDetailsCacheTest`에서 `MeterRegistry`에 `cache.gets{cache=userDetails,result=hit}`가
있고 2회 호출 뒤 1 이상인지 본다. `/actuator/prometheus`는 test 프로필에서 노출 설정이
없으므로 레지스트리를 직접 본다.

판정: 위 테스트 초록.

### 5. 전체 검사 · 문서 (docs)

- `scripts/harness/verify.sh` 초록
- `docs/tech_spec.md`에 캐시 한 단락 (무엇을 · TTL · 무효화 방식 · 다중 인스턴스 한계).
  해당 절이 없으면 보안 절 끝에 붙인다

## 알려진 한계

- **벌크 JPQL·네이티브 UPDATE/DELETE는 리스너를 우회한다.** `users` · `user_permissions` ·
  `user_credentials` · `department_permissions`를 `@Modifying` 질의나 `JdbcTemplate`으로
  고치면 엔티티 콜백이 안 돌아 캐시가 안 지워진다. 그런 코드를 더하면 같은 트랜잭션에서
  `UserDetailsCacheEvictor`로 직접 지워야 한다. 같은 문장을 `UserDetailsCacheEvictor`
  클래스 주석에도 둔다.
- 커밋 직전에 옛 값을 읽은 요청이 `afterCompletion` 뒤에 캐시에 쓰면 옛 권한이 최대 TTL 5분 남는다 (실패 경로 표).
- 다중 인스턴스에서는 인스턴스마다 따로 논다 (이슈의 「알려진 한계」).

## 부하 측정

`docs/reports/221-auth-permission-cache/`에 재현 스크립트와 HTML 리포트를 둔다.
dev와 같은 사양의 임시 GCP VM(라벨 `purpose=loadtest-221`)에 합성 데이터만 넣어 잰다 —
dev의 디스크·스냅샷·행 데이터는 복사하지 않는다(가져온 것은 설정값과 테이블별 행 수뿐).
측정이 끝나면 `teardown.sh`로 VM·디스크를 지우고 남은 디스크·스냅샷이 없음을 확인해
PR `리뷰어에게`에 «삭제 완료»를 적는다.

## 안 하는 것

- **`departmentPermissions` 캐시 (이슈 체크박스)는 만들지 않는다.** `userDetails`가 hit이면
  부서 권한 조회 자체가 안 일어난다. miss일 때만 사용자당 5분에 한 번 나가는 쿼리 하나를
  줄이려고 캐시 하나·설정 클래스·엔티티 대신 문자열 목록을 돌려주는 Proxy API 변경이
  따라온다. 같은 부서 스무 명분 중복도 권한 문자열 몇 개다. 측정에서 miss가 많으면 그때 더한다.
- **`CacheConfig` 클래스**: 캐시가 하나라 yml로 끝난다.
- **서비스마다 `@CacheEvict`, `UserDeactivatedEvent` 수신부 evict**: 엔티티 리스너가 덮는다
  (`softDelete`도 `User` 업데이트로 flush된다).
- **Cloud Monitoring 수집**: 앱 Ops Agent 설정의 `metric_relabel_configs` 허용 목록
  (`scripts/gcp/05_app/01_install-app-service.sh:146`)에 `cache_*`가 없다. 이번에는
  `/actuator/prometheus`에 나오는 것까지만 확인한다. 적중률을 GCP에서 봐야 하면 별 이슈로.
- 리프레시 토큰, Redis, 권한 모델 변경 (이슈의 범위 밖).

## 같이 고치는 문서

- `docs/tech_spec.md` (5)
- 이 `plan.md`, 끝나면 `review.md`

## 건드리지 않는 파일

#240(lesson 겹침 질의 이름), #241(subject 일정)이 병렬로 진행 중이다.
`domain/lesson/**`, `domain/subject/**`와 그 테스트는 열지 않는다.
