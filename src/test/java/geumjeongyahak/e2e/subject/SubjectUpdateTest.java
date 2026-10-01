package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.path.json.JsonPath;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;

/** PATCH /subjects/{id} — 이름·설명 수정과 권한. */
@DisplayName("E2E: 과목 기본 정보 PATCH 수정 테스트")
public class SubjectUpdateTest extends SubjectBaseTest {

    private static final long CLASSROOM_1 = DEFAULT_CLASSROOM_ID;
    private static final long TEACHER_ID = DEFAULT_TEACHER_ID;
    private static final long NEW_TEACHER_ID = 3L;

    @Test
    @DisplayName("PATCH: 기본 정보만 수정 성공(200 OK)")
    void patchSubject_UpdateNameOnly_Success() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("name", "국어(수정)"),
            Map.entry("description", "기본 정보 수정")
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
            .body("description", is("기본 정보 수정"))
            .body("period", is(2))
            .log().all();
    }

    @Test
    @DisplayName("subject:manage:* 권한으로 과목 수정 성공(200 OK)")
    void patchSubject_Success_WithSubjectManagePermission() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("name", "관리 권한 수정")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(subjectManageAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{subjectId}", subjectId)
            .then()
            .statusCode(200)
            .body("id", is((int) subjectId))
            .body("name", is("관리 권한 수정"));
    }

    @Test
    @DisplayName("PATCH: 과목명이 공백이면 400 Bad Request")
    void patchSubject_BadRequest_WhenNameIsBlank() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("name", "   ")
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
    @DisplayName("PATCH: 일정 필드는 기본 정보 수정 대상이 아니므로 변경되지 않는다")
    void patchSubject_IgnoresScheduleFields() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

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
            .patch("/{subjectId}", subjectId)
            .then()
            .statusCode(200)
            .body("dayOfWeek", is("MONDAY"))
            .body("period", is(2))
            .body("startAt", is("2099-03-02"))
            .body("endAt", is("2099-06-30"))
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
