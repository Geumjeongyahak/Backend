package geumjeongyahak.e2e.classroom;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.classroom.v1.dto.request.CreateClassroomRequest;
import geumjeongyahak.domain.classroom.v1.dto.response.ClassroomDetailResponse;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


@DisplayName("E2E: Classroom 생성 테스트")
public class ClassroomCreateTest extends BaseClassroomTest {

    @Test
    @DisplayName("관리자 권한으로 Classroom 생성 성공(201 Created)")
    void createClassroom_Success() {
        String uniqueName = "Classroom_" + System.currentTimeMillis();
        var req = new CreateClassroomRequest(
            uniqueName,
            "WEEKDAY",
            "This is a test classroom."
        );
        var res = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(req)
        .when()
                .post()
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo(uniqueName))
                .body("type", equalTo(ClassroomType.WEEKDAY.toString()))
                .body("description", equalTo("This is a test classroom."))
                .log().all()
                .extract()
                .as(ClassroomDetailResponse.class);

        // cleanup을 위해 생성된 교실 등록
        this.testClassroomHelper.registerClassroom(res.id());
    }

    @Test
    @DisplayName("관리자 권한으로 유효하지 않은 데이터로 Classroom 생성 실패(400 Bad Request)")
    void createClassroom_BadRequest() {
        var invalidReqs = List.of(
                Map.of(), // 빈 요
                Map.of("name", "", "type", "WEEKDAY"), // 이름 누락
                Map.of("name", "A".repeat(101), "type", "WEEKDAY"), // 이름 길이 초과
                Map.of("name", "Valid Name"), // 타입 누락
                Map.of("name", "Valid Name", "type", "INVALID_TYPE") // 잘못된 타입
        );

        invalidReqs.forEach(invalidReq -> {
            given()
                    .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                    .contentType(ContentType.JSON)
                    .body(invalidReq)
                    .when()
                    .post()
                    .then()
                    .statusCode(400)
                    .body("code", notNullValue())
                    .body("code", equalTo("VAL001"))
                    .body("errors", notNullValue())
                    .body("errors.size()", greaterThan(0))
                    .log().body();
        });
    }

    @Test
    @DisplayName("인증 없이 Classroom 생성 실패(401 Unauthorized)")
    void createClassroom_Unauthorized() {
        String uniqueName = "Classroom_" + System.currentTimeMillis();
        var req = new CreateClassroomRequest(
                uniqueName,
                "WEEKEND",
                "This is another test classroom."
        );
        given()
                .contentType(ContentType.JSON)
                .body(req)
        .when()
                .post()
        .then()
                .statusCode(401)
                .log().all();
    };

    @Test
    @DisplayName("게스트 권한으로 Classroom 생성 실패(403 Forbidden)")
    void createClassroom_Forbidden() {
        String uniqueName = "Classroom_" + System.currentTimeMillis();
        var req = new CreateClassroomRequest(
                uniqueName,
                "WEEKEND",
                "This is another test classroom."
        );
        given()
                .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
                .contentType(ContentType.JSON)
                .body(req)
        .when()
                .post()
        .then()
                .statusCode(403)
                .log().all();
    };

    @DisplayName("중복된 이름으로 Classroom 생성 실패(409 Conflict)" )
    @Test
    void createClassroom_Conflict() {
        String duplicateName = "Duplicate_Classroom_" + System.currentTimeMillis();
        // 먼저 하나 생성
        testClassroomHelper.createTestClassroom(duplicateName, ClassroomType.WEEKDAY);

        var req = new CreateClassroomRequest(
                duplicateName,
                "WEEKEND",
                "This classroom has a duplicate name."
        );

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(req)
        .when()
                .post()
        .then()
                .statusCode(409)
                .body("code", equalTo("BIZ-03-001"))
                .log().all();
    }
}
