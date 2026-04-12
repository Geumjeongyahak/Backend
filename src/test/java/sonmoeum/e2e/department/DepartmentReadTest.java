package sonmoeum.e2e.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.department.entity.Department;
import sonmoeum.domain.users.entity.User;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: Department 조회 테스트")
class DepartmentReadTest extends DepartmentBaseTest {

    private Department testDepartment;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testDepartment = departmentTestHelper.createTestDepartment(
                "테스트부서_" + System.currentTimeMillis(),
                "테스트용 부서입니다."
        );
        User volunteerUser = userTestHelper.getUser(TEST_VOLUNTEER_USERNAME);
        departmentTestHelper.joinDepartment(volunteerUser, testDepartment);
    }

    @Test
    @DisplayName("Department 목록 조회 성공(200 OK)")
    void getAllDepartments_Success() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("departments", notNullValue())
            .body("departments", hasSize(greaterThanOrEqualTo(1)))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 Department 목록 조회 실패(401 Unauthorized)")
    void getAllDepartments_NoAuth_Unauthorized() {
        given()
        .when()
            .get()
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("Department 상세 조회 성공 - 사용자 목록 포함(200 OK)")
    void getDepartmentById_Success() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/{id}", testDepartment.getId())
        .then()
            .statusCode(200)
            .body("id", equalTo(testDepartment.getId().intValue()))
            .body("name", equalTo(testDepartment.getName()))
            .body("description", equalTo(testDepartment.getDescription()))
            .body("users", notNullValue())
            .body("users", hasSize(greaterThanOrEqualTo(1)))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 Department 조회 실패(404 Not Found)")
    void getDepartmentById_NotFound() {
        Long nonExistentId = 99999L;

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/{id}", nonExistentId)
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 Department 상세 조회 실패(401 Unauthorized)")
    void getDepartmentById_NoAuth_Unauthorized() {
        given()
        .when()
            .get("/{id}", testDepartment.getId())
        .then()
            .statusCode(401)
            .log().all();
    }
}
