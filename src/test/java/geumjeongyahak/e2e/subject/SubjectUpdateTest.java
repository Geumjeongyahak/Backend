package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.path.json.JsonPath;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("E2E: 과목 PATCH 수정 테스트")
public class SubjectUpdateTest extends SubjectBaseTest {

    private static final long CLASSROOM_1 = 1L;
    private static final long TEACHER_ID = 2L;
    private static final long NEW_TEACHER_ID = 3L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private long createSubjectWithoutTeacher() {
        Map<String, Object> request = new HashMap<>(createRequest(CLASSROOM_1, "미배정 과목", "MONDAY", 2));
        request.remove("teacherId");

        JsonPath created = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("teacherId", is((Object) null))
            .extract()
            .jsonPath();

        return created.getLong("id");
    }

    private void createLessonExchangeRequestForTeacher(long teacherId, String lessonDate) {
        createLessonExchangeRequestForTeacher(teacherId, lessonDate, "PENDING");
    }

    private void createLessonExchangeRequestForTeacher(long teacherId, String lessonDate, String status) {
        jdbcTemplate.update(
            """
            INSERT INTO lesson_exchange_requests (
                id, lesson_date, requested_by, title, classroom_name_snapshot, content, status, scope, expires_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            1L,
            lessonDate,
            teacherId,
            "교환 요청",
            "벚꽃반",
            "교환 요청 내용",
            status,
            "FULL",
            "2099-02-28 23:59:59"
        );
    }

    private void createLessonExchangeProposalForTeacher(long teacherId, String lessonDate) {
        createLessonExchangeRequestForTeacher(NEW_TEACHER_ID, lessonDate);
        jdbcTemplate.update(
            """
            INSERT INTO lesson_exchange_proposals (
                id, request_id, proposed_by, proposal_type, proposal_scope, lesson_date, content,
                classroom_name_snapshot, status
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            1L,
            1L,
            teacherId,
            "EXCHANGE",
            "FULL",
            lessonDate,
            "교환 제안 내용",
            "벚꽃반",
            "ACTIVE"
        );
    }

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
    @DisplayName("PATCH /teacher: 담당 교사를 변경하면 미래 수업 교사도 변경된다")
    void assignTeacher_UpdatesFutureLessons() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> request = Map.ofEntries(
            Map.entry("teacherId", NEW_TEACHER_ID)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((int) NEW_TEACHER_ID))
            .body("teacherName", is("김철수"))
            .body("teacherAssignedAt", notNullValue());

        Integer changedLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND teacher_id = ?",
            Integer.class,
            subjectId,
            NEW_TEACHER_ID
        );

        assertThat(changedLessonCount).isEqualTo(12);
    }

    @Test
    @DisplayName("PATCH /teacher: 미배정 과목에 교사를 배정하면 미래 수업을 생성한다")
    void assignTeacher_CreatesLessonsForUnassignedSubject() {
        long subjectId = createSubjectWithoutTeacher();

        Map<String, Object> request = Map.ofEntries(
            Map.entry("teacherId", NEW_TEACHER_ID)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((int) NEW_TEACHER_ID))
            .body("teacherAssignedAt", notNullValue());

        Integer lessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND teacher_id = ?",
            Integer.class,
            subjectId,
            NEW_TEACHER_ID
        );

        assertThat(lessonCount).isEqualTo(12);
    }

    @Test
    @DisplayName("PATCH /teacher: teacherId가 null이면 과목 담당 교사를 비우고 미래 수업을 삭제한다")
    void assignTeacher_ClearsSubjectTeacher() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> request = new HashMap<>();
        request.put("teacherId", null);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((Object) null))
            .body("teacherName", is((Object) null))
            .body("teacherAssignedAt", is((Object) null));

        Integer activeLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND is_deleted = FALSE",
            Integer.class,
            subjectId
        );
        Integer deletedLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND is_deleted = TRUE",
            Integer.class,
            subjectId
        );

        assertThat(activeLessonCount).isZero();
        assertThat(deletedLessonCount).isEqualTo(12);
    }

    @Test
    @DisplayName("PATCH /teacher: 담당 교사 해제 후 다시 배정하면 미래 수업을 새로 생성한다")
    void assignTeacher_RecreatesLessonsAfterClearingTeacher() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> clearRequest = new HashMap<>();
        clearRequest.put("teacherId", null);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(clearRequest)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200);

        Map<String, Object> assignRequest = Map.ofEntries(
            Map.entry("teacherId", NEW_TEACHER_ID)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(assignRequest)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((int) NEW_TEACHER_ID))
            .body("teacherAssignedAt", notNullValue());

        Integer activeLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND teacher_id = ? AND is_deleted = FALSE",
            Integer.class,
            subjectId,
            NEW_TEACHER_ID
        );
        Integer totalLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ?",
            Integer.class,
            subjectId
        );

        assertThat(activeLessonCount).isEqualTo(12);
        assertThat(totalLessonCount).isEqualTo(24);
    }

    @Test
    @DisplayName("PATCH /teacher: 미래 수업에 교환 요청이 있으면 담당 교사를 변경할 수 없다")
    void assignTeacher_Conflict_WhenFutureLessonHasExchangeRequest() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);
        createLessonExchangeRequestForTeacher(TEACHER_ID, "2099-03-02");

        Map<String, Object> request = Map.ofEntries(
            Map.entry("teacherId", NEW_TEACHER_ID)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("PATCH /teacher: 미래 수업에 교환 제안이 있으면 담당 교사를 해제할 수 없다")
    void assignTeacher_Conflict_WhenFutureLessonHasExchangeProposal() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);
        createLessonExchangeProposalForTeacher(TEACHER_ID, "2099-03-02");

        Map<String, Object> request = new HashMap<>();
        request.put("teacherId", null);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("PATCH /teacher: 완료된 교환 요청은 담당 교사 변경을 막지 않는다")
    void assignTeacher_Success_WhenFutureLessonHasCompletedExchangeRequest() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);
        createLessonExchangeRequestForTeacher(TEACHER_ID, "2099-03-02", "COMPLETED");

        Map<String, Object> request = Map.ofEntries(
            Map.entry("teacherId", NEW_TEACHER_ID)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((int) NEW_TEACHER_ID));
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
