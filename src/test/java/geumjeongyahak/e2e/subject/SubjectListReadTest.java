package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 과목 목록 조회 테스트")
public class SubjectListReadTest extends SubjectBaseTest {

    // 시드에 존재하는 분반/유저 ID
    private static final long CLASSROOM_1 = 1L;
    private static final long CLASSROOM_2 = 2L;
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
            Map.entry("description", "목록 조회 테스트")
        );
    }

    private void createSubject(long classroomId, String name, String dayOfWeek, int period) {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(createRequest(classroomId, name, dayOfWeek, period))
            .when()
            .post()
            .then()
            .statusCode(201);
    }

    @Test
    @DisplayName("전체 과목 목록 조회 성공(200 OK)")
    void getSubjects_All_Success() {
        createSubject(CLASSROOM_1, "국어", "MONDAY", 2);
        createSubject(CLASSROOM_2, "수학", "TUESDAY", 1);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("size()", is(2))
            .log().all();
    }

    @Test
    @DisplayName("분반별 과목 목록 조회 성공(200 OK)")
    void getSubjects_ByClassroom_Success() {
        createSubject(CLASSROOM_1, "국어", "MONDAY", 2);
        createSubject(CLASSROOM_2, "수학", "TUESDAY", 1);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("classroomId", CLASSROOM_1)
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("size()", is(1))
            .body("[0].classroomId", is((int) CLASSROOM_1))
            .body("[0].classroomName", is("벚꽃반"))
            .body("[0].teacherId", is((int) TEACHER_ID))
            .body("[0].teacherName", is("홍길동"))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 분반으로 필터링 조회 시 404 Not Found")
    void getSubjects_ByClassroom_NotFound() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("classroomId", 999999)
            .when()
            .get()
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 과목 목록 조회 실패(401 Unauthorized)")
    void getSubjects_Unauthorized() {
        given()
            .when()
            .get()
            .then()
            .statusCode(401)
            .log().all();
    }
}
