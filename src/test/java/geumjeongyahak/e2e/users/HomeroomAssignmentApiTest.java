package geumjeongyahak.e2e.users;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.v1.dto.request.AssignUserHomeroomRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("E2E: 담임 업무분장 API 테스트")
class HomeroomAssignmentApiTest extends UserBaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("담임을 지정하면 부서 업무분장이 해제되고 조회 응답에 담임 분반이 표시된다")
    void assignHomeroom_releasesDepartmentAndReturnsWorkAssignment() {
        User volunteer = createUser("homeroom-api-department@test.com", RoleType.VOLUNTEER);
        jdbcTemplate.update(
            "UPDATE users SET department_id = 1, classroom_id = 2 WHERE id = ?",
            volunteer.getId()
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", volunteer.getId())
        .then()
            .statusCode(200)
            .body("workAssignment.type", equalTo("DEPARTMENT"))
            .body("workAssignment.departmentId", equalTo(1));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new AssignUserHomeroomRequest(1L))
        .when()
            .put("/{userId}/homeroom", volunteer.getId())
        .then()
            .statusCode(200)
            .body("department", nullValue())
            .body("classroom.id", equalTo(2))
            .body("workAssignment.type", equalTo("HOMEROOM"))
            .body("workAssignment.name", equalTo("담임"))
            .body("workAssignment.classroomId", equalTo(1))
            .body("workAssignment.classroomName", equalTo("벚꽃반"));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", volunteer.getId())
        .then()
            .statusCode(200)
            .body("department", nullValue())
            .body("classroom.id", equalTo(2))
            .body("workAssignment.type", equalTo("HOMEROOM"))
            .body("workAssignment.classroomId", equalTo(1));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("name", "homeroom-api-department")
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content[0].classroomId", equalTo(2))
            .body("content[0].workAssignment.type", equalTo("HOMEROOM"))
            .body("content[0].workAssignment.name", equalTo("담임"))
            .body("content[0].workAssignment.classroomName", equalTo("벚꽃반"));
    }

    @Test
    @DisplayName("같은 분반에 새 담임을 지정하면 기존 담임이 자동 해제된다")
    void assignHomeroom_replacesPreviousHomeroom() {
        User previous = createUser("homeroom-api-previous@test.com", RoleType.VOLUNTEER);
        User replacement = createUser("homeroom-api-replacement@test.com", RoleType.MANAGER);
        assignHomeroom(previous.getId(), 1L);

        assignHomeroom(replacement.getId(), 1L);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", previous.getId())
        .then()
            .statusCode(200)
            .body("workAssignment", nullValue());

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", replacement.getId())
        .then()
            .statusCode(200)
            .body("workAssignment.type", equalTo("HOMEROOM"))
            .body("workAssignment.classroomId", equalTo(1));
    }

    @Test
    @DisplayName("담임 업무분장을 명시적으로 해제할 수 있다")
    void releaseHomeroom_success() {
        User volunteer = createUser("homeroom-api-release@test.com", RoleType.VOLUNTEER);
        assignHomeroom(volunteer.getId(), 2L);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}/homeroom", volunteer.getId())
        .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", volunteer.getId())
        .then()
            .statusCode(200)
            .body("workAssignment", nullValue());
    }

    @Test
    @DisplayName("일반 사용자는 담임 업무분장을 지정하거나 해제할 수 없다")
    void homeroomAssignment_forbiddenForVolunteer() {
        User target = createUser("homeroom-api-forbidden@test.com", RoleType.VOLUNTEER);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(new AssignUserHomeroomRequest(1L))
        .when()
            .put("/{userId}/homeroom", target.getId())
        .then()
            .statusCode(403);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .delete("/{userId}/homeroom", target.getId())
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("GUEST 역할 사용자는 담임으로 지정할 수 없다")
    void assignHomeroom_rejectsGuest() {
        User guest = createUser("homeroom-api-guest@test.com", RoleType.GUEST);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new AssignUserHomeroomRequest(1L))
        .when()
            .put("/{userId}/homeroom", guest.getId())
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL002"));
    }

    private User createUser(String email, RoleType role) {
        return userTestHelper.createTestUser(email, email.substring(0, email.indexOf('@')), "password123!", role);
    }

    private void assignHomeroom(Long userId, Long classroomId) {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new AssignUserHomeroomRequest(classroomId))
        .when()
            .put("/{userId}/homeroom", userId)
        .then()
            .statusCode(200);
    }
}
