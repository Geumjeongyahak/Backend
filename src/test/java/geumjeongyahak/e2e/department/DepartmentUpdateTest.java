package geumjeongyahak.e2e.department;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.enums.DepartmentRoleType;
import geumjeongyahak.domain.department.v1.dto.request.DepartmentPermissionRequest;
import geumjeongyahak.domain.department.v1.dto.request.UpdateDepartmentRequest;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("E2E: Department 수정 테스트")
class DepartmentUpdateTest extends DepartmentBaseTest {

    private Department testDepartment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpDepartment() {
        testDepartment = departmentTestHelper.createTestDepartment(
                "수정테스트부서_" + System.currentTimeMillis(),
                "수정 전 설명"
        );
    }

    @Test
    @DisplayName("관리자 권한으로 Department 수정 성공 - 전체 필드(200 OK)")
    void updateDepartment_Success_AllFields() {
        UpdateDepartmentRequest req = new UpdateDepartmentRequest(
                "수정된 부서명",
                "수정된 설명",
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .put("/{id}", testDepartment.getId())
        .then()
            .statusCode(200)
            .body("id", equalTo(testDepartment.getId().intValue()))
            .body("name", equalTo("수정된 부서명"))
            .body("description", equalTo("수정된 설명"))
            .log().all();
    }

    @Test
    @DisplayName("관리자 권한으로 Department 수정 성공 - 이름만(200 OK)")
    void updateDepartment_Success_NameOnly() {
        UpdateDepartmentRequest req = new UpdateDepartmentRequest(
                "이름만 수정",
                null,
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .put("/{id}", testDepartment.getId())
        .then()
            .statusCode(200)
            .body("name", equalTo("이름만 수정"))
            .body("description", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("관리자 권한으로 Department 수정 성공 - 설명만(200 OK)")
    void updateDepartment_Success_DescriptionOnly() {
        UpdateDepartmentRequest req = new UpdateDepartmentRequest(
                null,
                "설명만 수정",
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .put("/{id}", testDepartment.getId())
        .then()
            .statusCode(200)
            .body("description", equalTo("설명만 수정"))
            .body("name", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("부서 권한 수정 시 roleType 기본값을 적용하고 기존 권한을 전체 교체한다(200 OK)")
    void updateDepartment_Permissions_ReplacesAllWithRoleType() {
        UpdateDepartmentRequest firstReq = new UpdateDepartmentRequest(
            null,
            null,
            List.of(
                new DepartmentPermissionRequest(null, "channel:write:*"),
                new DepartmentPermissionRequest(DepartmentRoleType.MANAGER, "channel:manage:*")
            )
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(firstReq)
        .when()
            .put("/{id}", testDepartment.getId())
        .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{id}", testDepartment.getId())
        .then()
            .statusCode(200)
            .body("permissions.size()", equalTo(2))
            .body("permissions.find { it.code == 'channel:write:*' }.source", equalTo("MEMBER"))
            .body("permissions.find { it.code == 'channel:manage:*' }.source", equalTo("MANAGER"));

        UpdateDepartmentRequest replaceReq = new UpdateDepartmentRequest(
            null,
            null,
            List.of(new DepartmentPermissionRequest(DepartmentRoleType.MANAGER, "event:manage:*"))
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(replaceReq)
        .when()
            .put("/{id}", testDepartment.getId())
        .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{id}", testDepartment.getId())
        .then()
            .statusCode(200)
            .body("permissions.size()", equalTo(1))
            .body("permissions.find { it.code == 'event:manage:*' }.source", equalTo("MANAGER"))
            .body("permissions.find { it.code == 'channel:write:*' }", nullValue())
            .body("permissions.find { it.code == 'channel:manage:*' }", nullValue())
            .log().all();

        Integer permissionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM department_permissions WHERE department_id = ?",
            Integer.class,
            testDepartment.getId()
        );
        org.assertj.core.api.Assertions.assertThat(permissionCount).isEqualTo(1);
    }

    @Test
    @DisplayName("일반 사용자 권한으로 Department 수정 실패(403 Forbidden)")
    void updateDepartment_Forbidden() {
        UpdateDepartmentRequest req = new UpdateDepartmentRequest(
                "수정 시도",
                "금지된 수정",
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .put("/{id}", testDepartment.getId())
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 Department 수정 실패(401 Unauthorized)")
    void updateDepartment_Unauthorized() {
        UpdateDepartmentRequest req = new UpdateDepartmentRequest(
                "수정 시도",
                "인증 없음",
                null
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .put("/{id}", testDepartment.getId())
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 Department 수정 실패(404 Not Found)")
    void updateDepartment_NotFound() {
        Long nonExistentId = 99999L;
        UpdateDepartmentRequest req = new UpdateDepartmentRequest(
                "수정 시도",
                "존재하지 않음",
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .put("/{id}", nonExistentId)
        .then()
            .statusCode(404)
            .log().all();
    }
}
