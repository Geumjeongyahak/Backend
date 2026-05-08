package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.path.json.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 과목 삭제(비활성화) 테스트")
public class SubjectDeleteTest extends SubjectBaseTest {

    private static final long CLASSROOM_1 = 1L;
    private static final long TEACHER_ID = 2L;

    private Map<String, Object> createRequest() {
        return Map.ofEntries(
            Map.entry("classroomId", CLASSROOM_1),
            Map.entry("teacherId", TEACHER_ID),
            Map.entry("name", "삭제 테스트 과목"),
            Map.entry("startAt", "2099-03-02"),
            Map.entry("endAt", "2099-06-30"),
            Map.entry("times", 12),
            Map.entry("dayOfWeek", "MONDAY"),
            Map.entry("startTime", "19:20:00"),
            Map.entry("endTime", "20:00:00"),
            Map.entry("period", 2),
            Map.entry("description", "삭제 테스트")
        );
    }

    private long createSubject() {
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

        return created.getLong("id");
    }

    @Test
    @DisplayName("관리자 과목 삭제(비활성화) 성공(204 No Content)")
    void deleteSubject_Success() {
        long subjectId = createSubject();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{subjectId}", subjectId)
            .then()
            .statusCode(204);

        // 단건 조회로 isActive=false 확인 (단건 조회가 비활성도 보여준다는 전제)
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{subjectId}", subjectId)
            .then()
            .statusCode(200)
            .body("isActive", is(false))
            .log().all();
    }

    @Test
    @DisplayName("subject:manage:* 권한으로 과목 삭제(비활성화) 성공(204 No Content)")
    void deleteSubject_Success_WithSubjectManagePermission() {
        long subjectId = createSubject();

        given()
            .header(AUTH_HEADER, getAuthHeader(subjectManageAccessToken))
            .when()
            .delete("/{subjectId}", subjectId)
            .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("존재하지 않는 과목 삭제 시 404 Not Found")
    void deleteSubject_NotFound() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{subjectId}", 999999)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("권한 없는 사용자 삭제 실패(403 Forbidden)")
    void deleteSubject_Forbidden() {
        long subjectId = createSubject();

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .delete("/{subjectId}", subjectId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("인증 없이 삭제 실패(401 Unauthorized)")
    void deleteSubject_Unauthorized() {
        long subjectId = createSubject();

        given()
            .when()
            .delete("/{subjectId}", subjectId)
            .then()
            .statusCode(401);
    }
}
