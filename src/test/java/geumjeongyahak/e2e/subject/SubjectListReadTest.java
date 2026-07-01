package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;
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

    private void createUnassignedSubject(long classroomId, String name, String dayOfWeek, int period) {
        Map<String, Object> request = new HashMap<>(createRequest(classroomId, name, dayOfWeek, period));
        request.remove("teacherId");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
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
    @DisplayName("과목 목록 조회 시 비활성 과목은 제외한다")
    void getSubjects_ExcludesInactiveSubjects() {
        createSubject(CLASSROOM_1, "활성 과목", "MONDAY", 2);
        Integer inactiveSubjectId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(createRequest(CLASSROOM_1, "비활성 과목", "TUESDAY", 1))
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{subjectId}", inactiveSubjectId)
        .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("size()", is(1))
            .body("[0].name", is("활성 과목"))
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
    @DisplayName("교사 미배정 과목 목록 조회 성공(200 OK)")
    void getUnassignedSubjects_Success() {
        createUnassignedSubject(CLASSROOM_1, "교사 미배정 국어", "MONDAY", 2);
        createUnassignedSubject(CLASSROOM_2, "교사 미배정 수학", "TUESDAY", 1);
        createSubject(CLASSROOM_1, "담당 교사 배정 과목", "WEDNESDAY", 3);

        given()
            .header(AUTH_HEADER, getAuthHeader(subjectManageAccessToken))
            .when()
            .get("/unassigned")
            .then()
            .statusCode(200)
            .body("size()", is(2))
            .body("[0].teacherId", nullValue())
            .body("[0].teacherName", nullValue())
            .body("[0].teacherAssignedAt", nullValue())
            .body("[0].classroomId", is((int) CLASSROOM_1))
            .body("[1].classroomId", is((int) CLASSROOM_2))
            .log().all();
    }

    @Test
    @DisplayName("게스트도 인증된 사용자면 교사 미배정 과목 목록 조회 성공(200 OK)")
    void getUnassignedSubjects_Success_Guest() {
        createUnassignedSubject(CLASSROOM_1, "교사 미배정 국어", "MONDAY", 2);

        given()
            .header(AUTH_HEADER, getAuthHeader(userTestHelper.generateAccessTokenByUserKey("guest01")))
            .when()
            .get("/unassigned")
            .then()
            .statusCode(200)
            .body("size()", is(1))
            .body("[0].teacherId", nullValue())
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
