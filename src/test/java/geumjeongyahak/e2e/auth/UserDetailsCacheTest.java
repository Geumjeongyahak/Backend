package geumjeongyahak.e2e.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.e2e.BaseE2ETest;
import geumjeongyahak.e2e.util.TestDepartmentHelper;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("E2E: 인증 사용자 캐시")
class UserDetailsCacheTest extends BaseE2ETest {

    // GET /api/v1/users 는 ADMIN 역할 또는 user:read:* 권한이 있어야 열린다
    private static final String GUARDED = "/api/v1/users";
    private static final String USER_READ = "user:read:*";

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private TestDepartmentHelper departmentTestHelper;

    @AfterEach
    @Override
    protected void tearDown() {
        super.tearDown();
        departmentTestHelper.clearAll();
    }

    @Test
    @DisplayName("같은 토큰의 두 번째 요청은 사용자·권한 테이블을 다시 읽지 않는다")
    void secondRequest_doesNotQueryAuthTables() {
        // 시드 사용자 2: 봉사자 · 부서 2 소속 · 개인 권한 1 → 인증 쿼리 넷이 다 나가는 사용자
        String token = userTestHelper.generateAccessToken(2L);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        getDepartments(token);
        long before = statistics.getPrepareStatementCount();
        getDepartments(token);

        // 부서 목록 본문 쿼리 하나만 남는다 (캐시 전: 인증 4 + 본문 1)
        assertThat(statistics.getPrepareStatementCount() - before).isEqualTo(1);
        assertThat(meterRegistry.get("cache.gets").tag("cache", "userDetails").tag("result", "hit")
            .functionCounter().count()).isPositive();
    }

    @Test
    @DisplayName("개인 권한을 회수하면 같은 토큰이 바로 막힌다")
    void revokeUserPermission_takesEffectImmediately() {
        User user = userTestHelper.createTestUser("cache-perm@test.com", RoleType.VOLUNTEER);
        given().header(AUTH_HEADER, adminAuth()).contentType(ContentType.JSON)
            .body(Map.of("permissionCode", USER_READ))
            .post("/api/v1/users/{userId}/permissions", user.getId())
            .then().statusCode(200);
        String token = userTestHelper.generateAccessToken(user.getId());
        expectGuarded(token, 200);

        given().header(AUTH_HEADER, adminAuth()).contentType(ContentType.JSON)
            .body(Map.of("permissionCode", USER_READ))
            .delete("/api/v1/users/{userId}/permissions", user.getId())
            .then().statusCode(200);

        expectGuarded(token, 403);
    }

    @Test
    @DisplayName("역할을 강등하면 같은 토큰이 바로 막힌다")
    void demoteRole_takesEffectImmediately() {
        User user = userTestHelper.createTestUser("cache-role@test.com", RoleType.ADMIN);
        String token = userTestHelper.generateAccessToken(user.getId());
        expectGuarded(token, 200);

        given().header(AUTH_HEADER, adminAuth()).contentType(ContentType.JSON)
            .body(Map.of("role", "VOLUNTEER"))
            .patch("/api/v1/users/{userId}", user.getId())
            .then().statusCode(200);

        expectGuarded(token, 403);
    }

    @Test
    @DisplayName("부서 권한 프리셋을 회수하면 소속원의 같은 토큰이 바로 막힌다")
    void revokeDepartmentPermission_takesEffectImmediately() {
        Department department = departmentTestHelper.createTestDepartment("캐시 검증 부서", "캐시 무효화 검증용");
        putDepartmentPermissions(department, List.of(Map.of("roleType", "MEMBER", "permissionCode", USER_READ)));
        User user = userTestHelper.createTestUser("cache-dept@test.com", RoleType.VOLUNTEER);
        departmentTestHelper.joinDepartment(user, department);
        String token = userTestHelper.generateAccessToken(user.getId());
        expectGuarded(token, 200);

        putDepartmentPermissions(department, List.of());

        expectGuarded(token, 403);
        // 정리: 소속을 풀어야 부서를 지울 수 있다
        given().header(AUTH_HEADER, adminAuth())
            .delete("/api/v1/users/{userId}/department", user.getId())
            .then().statusCode(204);
    }

    @Test
    @DisplayName("삭제된 사용자의 같은 토큰은 바로 막힌다")
    void deletedUser_isRejectedImmediately() {
        User user = userTestHelper.createTestUser("cache-delete@test.com", RoleType.VOLUNTEER);
        String token = userTestHelper.generateAccessToken(user.getId());
        given().header(AUTH_HEADER, getAuthHeader(token)).get("/api/v1/users/me").then().statusCode(200);

        given().header(AUTH_HEADER, adminAuth())
            .delete("/api/v1/users/{userId}", user.getId())
            .then().statusCode(204);

        given().header(AUTH_HEADER, getAuthHeader(token)).get("/api/v1/users/me").then().statusCode(401);
    }

    private void getDepartments(String token) {
        given().header(AUTH_HEADER, getAuthHeader(token)).get("/api/v1/departments").then().statusCode(200);
    }

    private void expectGuarded(String token, int status) {
        given().header(AUTH_HEADER, getAuthHeader(token)).get(GUARDED).then().statusCode(status);
    }

    private void putDepartmentPermissions(Department department, List<Map<String, String>> permissions) {
        given().header(AUTH_HEADER, adminAuth()).contentType(ContentType.JSON)
            .body(Map.of("name", department.getName(), "description", department.getDescription(), "permissions", permissions))
            .put("/api/v1/departments/{id}", department.getId())
            .then().statusCode(200);
    }

    private String adminAuth() {
        return getAuthHeader(userTestHelper.generateAccessTokenByEmail(TEST_ADMIN_EMAIL));
    }
}
