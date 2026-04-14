package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 과목 생성 테스트")
public class SubjectCreateTest extends SubjectBaseTest {

    private static final long CLASSROOM_ID = 1L;
    private static final long TEACHER_ID = 2L;

    private Map<String, Object> createRequest(String name, String startAt, String endAt, String dayOfWeek) {
        return Map.ofEntries(
            entry("classroomId", CLASSROOM_ID),
            entry("teacherId", TEACHER_ID),
            entry("name", name),
            entry("startAt", startAt),
            entry("endAt", endAt),
            entry("times", 12),
            entry("dayOfWeek", dayOfWeek),
            entry("startTime", "20:10:00"),
            entry("endTime", "20:50:00"),
            entry("period", 2),
            entry("description", "E2E 테스트 과목")
        );
    }

    @Test
    @DisplayName("관리자 권한으로 과목 생성 성공(201 Created)")
    void createSubject_Success_Admin() {
        Map<String, Object> request = createRequest(
            "국어",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", is("국어"))
            .body("period", is(2))
            .log().all();
    }

    @Test
    @DisplayName("일반 선생님 권한으로 과목 생성 실패(403 Forbidden)")
    void createSubject_Forbidden_Volunteer() {
        Map<String, Object> request = createRequest(
            "수학",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 과목 생성 실패(401 Unauthorized)")
    void createSubject_Unauthorized() {
        Map<String, Object> request = createRequest(
            "영어",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("검증 실패(기간 역전: startAt > endAt) 시 400 Bad Request")
    void createSubject_BadRequest_InvalidDateRange() {
        Map<String, Object> request = createRequest(
            "사회",
            "2026-06-30",
            "2026-03-02",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("검증 실패(시간 역전: startTime >= endTime) 시 400 Bad Request")
    void createSubject_BadRequest_InvalidTimeRange() {
        Map<String, Object> request = Map.of(
            "classroomId", CLASSROOM_ID,
            "teacherId", TEACHER_ID,
            "name", "과학",
            "startAt", "2026-03-02",
            "endAt", "2026-06-30",
            "times", 12,
            "dayOfWeek", "MONDAY",
            "startTime", "20:00:00",
            "endTime", "19:20:00",
            "period", 1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("기간이 겹치고 요일/교시가 같으면 생성 실패(409 Conflict)")
    void createSubject_Conflict_WhenOverlapAndSameSlot() {
        // 1) 먼저 하나 생성
        Map<String, Object> first = createRequest(
            "도덕",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(first)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue());

        // 2) 같은 반/요일/교시 + 기간 겹침 -> 409 기대
        Map<String, Object> conflict = createRequest(
            "미술",
            "2026-05-01",
            "2026-07-01",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(conflict)
            .when()
            .post()
            .then()
            .statusCode(409)
            .log().all();
    }

    @Test
    @DisplayName("요일/교시가 같아도 기간이 겹치지 않으면 생성 성공(201 Created)")
    void createSubject_Success_WhenNotOverlapEvenSameSlot() {
        // 먼저 하나 생성
        Map<String, Object> first = createRequest(
            "음악",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(first)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue());

        // 같은 요일/교시지만 기간이 겹치지 않음 -> 성공
        Map<String, Object> second = createRequest(
            "체육",
            "2026-07-01",
            "2026-09-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(second)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", is("체육"))
            .log().all();
    }
}
