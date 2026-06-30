package geumjeongyahak.e2e.classroom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("E2E: Classroom 단건 조회 테스트")
public class ClassroomReadTest extends BaseClassroomTest {

    @Test
    @DisplayName("관리자 권한으로 Classroom 단건 조회 성공(200 OK)")
    void getClassroomById_Success_Admin() {
        // Given: 테스트용 교실 생성
        Classroom classroom = testClassroomHelper.createTestClassroom(
            "Test Classroom 1",
            ClassroomType.WEEKDAY,
            "관리자 단건 조회 테스트용 교실"
        );
        Long classroomId = classroom.getId();

        // When & Then: 관리자가 교실 조회
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{id}", classroomId)
        .then()
            .statusCode(200)
            .body("id", equalTo(classroomId.intValue()))
            .body("name", equalTo("Test Classroom 1"))
            .body("type", equalTo("WEEKDAY"))
            .body("description", equalTo("관리자 단건 조회 테스트용 교실"))
            .body("createdAt", notNullValue())
            .body("updatedAt", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("게스트 권한으로 Classroom 단건 조회 성공(200 OK)")
    void getClassroomById_Success_Guest() {
        // Given: 테스트용 교실 생성
        Classroom classroom = testClassroomHelper.createTestClassroom(
            "Test Classroom 2",
            ClassroomType.WEEKEND,
            "게스트 단건 조회 테스트용 교실"
        );
        Long classroomId = classroom.getId();

        // When & Then: 게스트가 교실 조회
        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
        .when()
            .get("/{id}", classroomId)
        .then()
            .statusCode(200)
            .body("id", equalTo(classroomId.intValue()))
            .body("name", equalTo("Test Classroom 2"))
            .body("type", equalTo("WEEKEND"))
            .body("description", equalTo("게스트 단건 조회 테스트용 교실"))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 Classroom 조회 실패(404 Not Found)")
    void getClassroomById_NotFound() {
        // Given: 존재하지 않는 ID
        Long nonExistentId = 99999L;

        // When & Then: 존재하지 않는 교실 조회
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{id}", nonExistentId)
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 Classroom 단건 조회 성공(200 OK)")
    void getClassroomById_Success_Unauthenticated() {
        // Given: 테스트용 교실 생성
        Classroom classroom = testClassroomHelper.createTestClassroom(
            "Test Classroom 3",
            ClassroomType.WEEKDAY,
            "인증 없는 조회 테스트용 교실"
        );
        Long classroomId = classroom.getId();

        // When & Then: 인증 없이 교실 조회
        given()
        .when()
            .get("/{id}", classroomId)
        .then()
            .statusCode(200)
            .body("id", equalTo(classroomId.intValue()))
            .body("name", equalTo("Test Classroom 3"))
            .body("type", equalTo("WEEKDAY"))
            .body("description", equalTo("인증 없는 조회 테스트용 교실"))
            .log().all();
    }
}
