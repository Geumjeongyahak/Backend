package sonmoeum.e2e.department;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.department.entity.Department;
import sonmoeum.domain.department.v1.dto.request.JoinDepartmentRequest;
import sonmoeum.domain.users.entity.User;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@DisplayName("E2E: Department 삭제 테스트")
class DepartmentDeleteTest extends DepartmentBaseTest {

    private Department emptyDepartment;
    private Department departmentWithMembers;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        // 멤버가 없는 부서
        emptyDepartment = departmentTestHelper.createTestDepartment(
                "빈부서_" + System.currentTimeMillis(),
                "멤버가 없는 부서"
        );

        // 멤버가 있는 부서
        departmentWithMembers = departmentTestHelper.createTestDepartment(
                "멤버있는부서_" + System.currentTimeMillis(),
                "멤버가 있는 부서"
        );

        User volunteerUser = userTestHelper.getUser(TEST_VOLUNTEER_USERNAME);
        departmentTestHelper.joinDepartment(volunteerUser, departmentWithMembers);
    }

    @Test
    @DisplayName("관리자 권한으로 빈 Department 삭제 성공(204 No Content)")
    void deleteDepartment_Success_EmptyDepartment() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{id}", emptyDepartment.getId())
        .then()
            .statusCode(204)
            .log().all();

        // 삭제 확인
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{id}", emptyDepartment.getId())
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("멤버가 있는 Department 삭제 실패(400 Bad Request)")
    void deleteDepartment_Fail_HasMembers() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{id}", departmentWithMembers.getId())
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 Department 삭제 실패(403 Forbidden)")
    void deleteDepartment_Forbidden() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .delete("/{id}", emptyDepartment.getId())
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 Department 삭제 실패(401 Unauthorized)")
    void deleteDepartment_Unauthorized() {
        given()
        .when()
            .delete("/{id}", emptyDepartment.getId())
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 Department 삭제 실패(404 Not Found)")
    void deleteDepartment_NotFound() {
        Long nonExistentId = 99999L;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{id}", nonExistentId)
        .then()
            .statusCode(404)
            .log().all();
    }
}
