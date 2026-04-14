package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.path.json.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 과목 단건 조회 테스트")
public class SubjectReadTest extends SubjectBaseTest {

    private static final long CLASSROOM_ID = 1L;
    private static final long TEACHER_ID = 2L;

    private Map<String, Object> createRequest() {
        return Map.ofEntries(
            Map.entry("classroomId", CLASSROOM_ID),
            Map.entry("teacherId", TEACHER_ID),
            Map.entry("name", "국어"),
            Map.entry("startAt", "2099-03-02"),
            Map.entry("endAt", "2099-06-30"),
            Map.entry("times", 12),
            Map.entry("dayOfWeek", "MONDAY"),
            Map.entry("startTime", "19:20:00"),
            Map.entry("endTime", "20:00:00"),
            Map.entry("period", 2),
            Map.entry("description", "단건 조회 테스트")
        );
    }

    @Test
    @DisplayName("과목 단건 조회 성공(200 OK)")
    void getSubject_Success() {
        JsonPath created = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(createRequest())
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .jsonPath();

        long subjectId = created.getLong("id");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{subjectId}", subjectId)
            .then()
            .statusCode(200)
            .body("id", is((int) subjectId))
            .body("classroomId", is((int) CLASSROOM_ID))
            .body("teacherId", is((int) TEACHER_ID))
            .body("name", is("국어"))
            .body("dayOfWeek", is("MONDAY"))
            .body("period", is(2))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 과목 단건 조회 시 404 Not Found")
    void getSubject_NotFound() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{subjectId}", 999999)
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 과목 단건 조회 실패(401 Unauthorized)")
    void getSubject_Unauthorized() {
        // 먼저 admin으로 과목 생성
        long subjectId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(createRequest())
            .when()
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        // 토큰 없이 조회
        given()
            .when()
            .get("/{subjectId}", subjectId)
            .then()
            .statusCode(401)
            .log().all();
    }
}