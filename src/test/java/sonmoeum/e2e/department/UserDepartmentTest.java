package sonmoeum.e2e.department;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.department.entity.Department;
import sonmoeum.domain.department.v1.dto.request.JoinDepartmentRequest;
import sonmoeum.domain.users.entity.User;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: User-Department 관계 테스트")
class UserDepartmentTest extends DepartmentBaseTest {

    private Department testDepartment;
    private User volunteerUser;
    private String anotherVolunteerUsername;
    private String anotherVolunteerAccessToken;

    @BeforeEach
    void setUpUserDepartment() {
        // basePath를 users로 변경
        RestAssured.basePath = "/api/v1/users";

        // 테스트용 부서 생성
        testDepartment = departmentTestHelper.createTestDepartment(
                "유저부서테스트_" + System.currentTimeMillis(),
                "사용자-부서 관계 테스트용"
        );

        // 테스트용 사용자들
        volunteerUser = userTestHelper.getUser(TEST_VOLUNTEER_USERNAME);
        anotherVolunteerUsername = "another_volunteer_" + System.currentTimeMillis();
        userTestHelper.createTestUser(anotherVolunteerUsername, List.of(RoleType.ROLE_VOLUNTEER));
        anotherVolunteerAccessToken = userTestHelper.generateAccessToken(anotherVolunteerUsername);
    }

    @Test
    @DisplayName("본인이 부서 참여 성공(201 Created)")
    void joinDepartment_Success_Self() {
        JoinDepartmentRequest req = new JoinDepartmentRequest(testDepartment.getId());

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/departments", volunteerUser.getId())
        .then()
            .statusCode(201)
            .log().all();
    }

    @Test
    @DisplayName("관리자가 다른 사용자를 부서에 참여시킴(201 Created)")
    void joinDepartment_Success_ByAdmin() {
        User anotherUser = userTestHelper.getUser(anotherVolunteerUsername);
        JoinDepartmentRequest req = new JoinDepartmentRequest(testDepartment.getId());

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/departments", anotherUser.getId())
        .then()
            .statusCode(201)
            .log().all();
    }

    @Test
    @DisplayName("다른 사용자를 부서에 참여시키기 실패(403 Forbidden)")
    void joinDepartment_Forbidden_OtherUser() {
        User anotherUser = userTestHelper.getUser(anotherVolunteerUsername);
        JoinDepartmentRequest req = new JoinDepartmentRequest(testDepartment.getId());

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/departments", anotherUser.getId())
        .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("이미 참여한 부서에 다시 참여 시도 실패(409 Conflict)")
    void joinDepartment_Duplicate() {
        JoinDepartmentRequest req = new JoinDepartmentRequest(testDepartment.getId());

        // 첫 번째 참여
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/departments", volunteerUser.getId())
        .then()
            .statusCode(201);

        // 중복 참여 시도
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/departments", volunteerUser.getId())
        .then()
            .statusCode(409)
            .body("code", equalTo("BIZ001"))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 부서에 참여 시도 실패(404 Not Found)")
    void joinDepartment_DepartmentNotFound() {
        JoinDepartmentRequest req = new JoinDepartmentRequest(99999L);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/departments", volunteerUser.getId())
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 부서 참여 시도 실패(401 Unauthorized)")
    void joinDepartment_Unauthorized() {
        JoinDepartmentRequest req = new JoinDepartmentRequest(testDepartment.getId());

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/departments", volunteerUser.getId())
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("본인이 부서 탈퇴 성공(204 No Content)")
    void leaveDepartment_Success_Self() {
        // 먼저 참여
        departmentTestHelper.joinDepartment(volunteerUser, testDepartment);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .delete("/{userId}/departments/{departmentId}",
                    volunteerUser.getId(), testDepartment.getId())
        .then()
            .statusCode(204)
            .log().all();
    }

    @Test
    @DisplayName("관리자가 다른 사용자를 부서에서 탈퇴시킴(204 No Content)")
    void leaveDepartment_Success_ByAdmin() {
        User anotherUser = userTestHelper.getUser(anotherVolunteerUsername);
        departmentTestHelper.joinDepartment(anotherUser, testDepartment);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}/departments/{departmentId}",
                    anotherUser.getId(), testDepartment.getId())
        .then()
            .statusCode(204)
            .log().all();
    }

    @Test
    @DisplayName("참여하지 않은 부서에서 탈퇴 시도 실패(404 Not Found)")
    void leaveDepartment_NotJoined() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .delete("/{userId}/departments/{departmentId}",
                    volunteerUser.getId(), testDepartment.getId())
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("사용자 소속 부서 목록 조회 성공(200 OK)")
    void getUserDepartments_Success() {
        // 부서에 참여
        departmentTestHelper.joinDepartment(volunteerUser, testDepartment);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/{userId}/departments", volunteerUser.getId())
        .then()
            .statusCode(200)
            .body("departments", notNullValue())
            .body("departments", hasSize(greaterThanOrEqualTo(1)))
            .body("departments[0].id", equalTo(testDepartment.getId().intValue()))
            .log().all();
    }

    @Test
    @DisplayName("본인 소속 부서 목록 조회 성공(200 OK)")
    void getMyDepartments_Success() {
        // 부서에 참여
        departmentTestHelper.joinDepartment(volunteerUser, testDepartment);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/me/departments")
        .then()
            .statusCode(200)
            .body("departments", notNullValue())
            .body("departments", hasSize(greaterThanOrEqualTo(1)))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 본인 부서 목록 조회 실패(401 Unauthorized)")
    void getMyDepartments_Unauthorized() {
        given()
        .when()
            .get("/me/departments")
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 부서 목록 조회 실패(404 Not Found)")
    void getUserDepartments_UserNotFound() {
        Long nonExistentUserId = 99999L;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}/departments", nonExistentUserId)
        .then()
            .statusCode(404)
            .log().all();
    }
}
