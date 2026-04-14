package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.path.json.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 과목 PATCH 수정 테스트")
public class SubjectUpdateTest extends SubjectBaseTest {

    private static final long CLASSROOM_1 = 1L;
    private static final long TEACHER_ID = 2L;

    private Map<String, Object> createRequest(long classroomId, String name, String dayOfWeek, int period) {
        return Map.ofEntries(
            Map.entry("classroomId", classroomId),
            Map.entry("teacherId", TEACHER_ID),
            Map.entry("name", name),
            Map.entry("startAt", "2099-03-02"),
            Map.entry("endAt", "2099-06-30"),
            Map.entry("times", 12),
            Map.entry("dayOfWeek", dayOfWeek),
            Map.entry("startTime", "19:20:00"),
            Map.entry("endTime", "20:00:00"),
            Map.entry("period", period),
            Map.entry("description", "PATCH 테스트")
        );
    }

    private long createSubject(long classroomId, String name, String dayOfWeek, int period) {
        JsonPath created = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(createRequest(classroomId, name, dayOfWeek, period))
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .jsonPath();

        return created.getLong("id");
    }

    @Test
    @DisplayName("PATCH: 이름만 수정 성공(200 OK)")
    void patchSubject_UpdateNameOnly_Success() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("name", "국어(수정)")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{subjectId}", subjectId)
            .then()
            .statusCode(200)
            .body("id", is((int) subjectId))
            .body("name", is("국어(수정)"))
            .log().all();
    }

    @Test
    @DisplayName("PATCH: 스케줄 시간 역전이면 400 Bad Request")
    void patchSubject_InvalidTimeRange_BadRequest() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("startTime", "20:00:00"),
            Map.entry("endTime", "19:20:00")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{subjectId}", subjectId)
            .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("PATCH: 다른 과목과 중복되는 스케줄로 수정 시 409 Conflict")
    void patchSubject_Conflict() {
        // A 과목: 월 2교시, 2099-03-02 ~ 2099-06-30
        createSubject(CLASSROOM_1, "A", "MONDAY", 2);

        // B: 다른 슬롯
        long b = createSubject(CLASSROOM_1, "B", "TUESDAY", 1);

        // B를 A와 같은 슬롯으로 + A 기간과 겹치게 수정 => 409
        Map<String, Object> patch = Map.ofEntries(
            Map.entry("dayOfWeek", "MONDAY"),
            Map.entry("period", 2),
            Map.entry("startAt", "2099-05-01"),
            Map.entry("endAt", "2099-07-01")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{subjectId}", b)
            .then()
            .statusCode(409)
            .log().all();
    }

    @Test
    @DisplayName("PATCH: 권한 없는 사용자 수정 실패(403 Forbidden)")
    void patchSubject_Forbidden_Volunteer() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("name", "수정시도")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{subjectId}", subjectId)
            .then()
            .statusCode(403)
            .log().all();
    }
}
