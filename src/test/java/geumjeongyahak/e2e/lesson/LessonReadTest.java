package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.enums.RoleType;

@DisplayName("E2E: 수업 조회 테스트")
public class LessonReadTest extends LessonBaseTest {

    // [전체 수업 목록 조회 테스트]

    @Test
    @DisplayName("관리자 권한으로 전체 수업 목록(기간 조회) 성공(200 OK)")
    void getAllLessons_Success_Admin() {
        createTrackedLessonFixture("read-list-admin-a", TEACHER_ID, "2042-05-12", "MONDAY", 1, "2027-05-12", "19:20:00", "20:00:00", 1);
        createTrackedLessonFixture("read-list-admin-b", TEACHER2_ID, "2042-05-13", "TUESDAY", 2, "2027-05-13", "20:10:00", "20:50:00", 2);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("from", "2027-05-01")
            .queryParam("to", "2027-05-31")
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("$", is(notNullValue()))
            .body("size()", is(2))
            .body("lessonId", everyItem(notNullValue()))
            .body("date", everyItem(notNullValue()))
            .body("period", everyItem(allOf(notNullValue(), anyOf(is(1), is(2), is(3)))))
            .body("classroomId", everyItem(notNullValue()))
            .body("classroomName", everyItem(notNullValue()))
            .body("[0].date", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("일반 선생님 권한으로 전체 수업 목록(기간 조회) 성공(200 OK)")
    void getAllLessons_Success_Volunteer() {
        createTrackedLessonFixture("read-list-volunteer", TEACHER_ID, "2042-05-14", "WEDNESDAY", 3, "2027-05-14", "19:20:00", "20:00:00", 1);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .queryParam("from", "2027-05-01")
            .queryParam("to", "2027-05-31")
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("$", is(notNullValue()))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 전체 수업 목록 조회 성공(200 OK)")
    void getAllLessons_PublicSuccess() {
        createTrackedLessonFixture("read-list-public", TEACHER_ID, "2042-05-15", "THURSDAY", 4, "2027-05-15", "19:20:00", "20:00:00", 1);

        given()
            .queryParam("from", "2027-05-01")
            .queryParam("to", "2027-05-31")
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("$", is(notNullValue()))
            .body("lessonId", everyItem(notNullValue()))
            .body("teacherName", everyItem(notNullValue()))
            .body("subjectName", everyItem(notNullValue()))
            .body("classroomId", everyItem(notNullValue()))
            .body("classroomName", everyItem(notNullValue()))
            .log().all();
    }

    @Test
    @DisplayName("기간 교차 검증 실패(from > to) 시 400 Bad Request")
    void getAllLessons_InvalidRange_BadRequest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("from", "2026-03-02")
            .queryParam("to", "2026-02-01")
            .when()
            .get()
            .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("월간 캘린더 그리드 범위(42일) 조회 성공(200 OK)")
    void getAllLessons_MaxCalendarRange_Success() {
        createTrackedLessonFixture("read-list-calendar-range", TEACHER_ID, "2042-05-16", "FRIDAY", 5, "2026-03-14", "19:20:00", "20:00:00", 1);

        given()
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-03-14")
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("$", is(notNullValue()))
            .log().all();
    }

    @Test
    @DisplayName("기간 범위 초과(42일 초과) 시 400 Bad Request")
    void getAllLessons_RangeTooLarge_BadRequest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            // 2026-02-01 ~ 2026-03-15 (43일) => 범위 초과
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-03-15")
            .when()
            .get()
            .then()
            .statusCode(400)
            .log().all();
    }

    // [내 수업 목록 조회 테스트]

    @Test
    @DisplayName("내 수업 목록 조회 성공(200 OK) - 로그인 사용자")
    void getMyLessons_Success() {
        createTrackedLessonFixture("read-my-a", TEACHER_ID, "2042-06-02", "MONDAY", 1, "2027-06-02", "19:20:00", "20:00:00", 1);
        createTrackedLessonFixture("read-my-b", TEACHER_ID, "2042-06-03", "TUESDAY", 2, "2027-06-03", "20:10:00", "20:50:00", 2);
        createTrackedLessonFixture("read-my-other", TEACHER2_ID, "2042-06-04", "WEDNESDAY", 3, "2027-06-04", "19:20:00", "20:00:00", 1);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .queryParam("from", "2027-06-01")
            .queryParam("to", "2027-06-30")
            .when()
            .get("/me")
            .then()
            .statusCode(200)
            .body("$", notNullValue())
            .body("size()", is(2))
            .body("lessonId", everyItem(notNullValue()))
            .body("date", everyItem(notNullValue()))
            .body("period", everyItem(anyOf(is(1), is(2), is(3))))
            .body("classroomId", everyItem(is((int) CLASSROOM_ID)))
            .body("classroomName", everyItem(notNullValue()))
            .log().all();
    }

    @Test
    @DisplayName("내 수업 목록 조회 실패(401 Unauthorized) - 인증 없음")
    void getMyLessons_Unauthorized() {
        given()
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-02-28")
            .when()
            .get("/me")
            .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("내 수업 목록 조회 실패(400 Bad Request) - from > to")
    void getMyLessons_InvalidRange_BadRequest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .queryParam("from", "2026-03-02")
            .queryParam("to", "2026-02-01")
            .when()
            .get("/me")
            .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("내 수업 목록 조회 실패(400 Bad Request) - 기간 범위 초과")
    void getMyLessons_RangeTooLarge_BadRequest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            // 42일 초과로 요청
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-03-15")
            .when()
            .get("/me")
            .then()
            .statusCode(400)
            .log().all();
    }

    // [수업 상세 조회 테스트]

    @Test
    @DisplayName("관리자는 타인의 수업 상세 조회 가능(200 OK)")
    void getLessonDetail_Admin_CanAccessOthersLesson() {
        long othersLessonId = createTrackedLessonFixture(
            "read-detail-admin",
            TEACHER2_ID,
            "2042-07-07",
            "MONDAY",
            1,
            "2027-07-07",
            "19:20:00",
            "20:00:00",
            1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{lessonId}", othersLessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) othersLessonId))
            .body("date", notNullValue())
            .body("period", anyOf(is(1), is(2), is(3)))
            .body("startTime", notNullValue())
            .body("endTime", notNullValue())
            .body("status", notNullValue())
            .body("teacherName", notNullValue())
            .body("subjectName", notNullValue())
            .body("dailyScheduleId", notNullValue())
            .body("classroomId", is((int) CLASSROOM_ID))
            .body("classroomName", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("교사는 본인 수업 상세 조회 가능(200 OK)")
    void getLessonDetail_Volunteer_CanAccessMyLesson() {
        long myLessonId = createTrackedLessonFixture(
            "read-detail-my",
            TEACHER_ID,
            "2042-07-08",
            "TUESDAY",
            2,
            "2027-07-08",
            "20:10:00",
            "20:50:00",
            2
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get("/{lessonId}", myLessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) myLessonId))
            .body("date", notNullValue())
            .body("period", anyOf(is(1), is(2), is(3)))
            .body("status", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("lesson:read:* 권한으로 타인 수업 상세 조회 가능(200 OK)")
    void getLessonDetail_LessonReadPermission_CanAccessOthersLesson() {
        long othersLessonId = createTrackedLessonFixture(
            "read-detail-permission",
            TEACHER2_ID,
            "2042-07-10",
            "THURSDAY",
            4,
            "2027-07-10",
            "19:20:00",
            "20:00:00",
            1
        );
        String lessonReadToken = createAccessTokenWithPermission(
            "lesson-read-detail",
            RoleType.VOLUNTEER,
            "lesson:read:*"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(lessonReadToken))
            .when()
            .get("/{lessonId}", othersLessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) othersLessonId))
            .body("teacherName", notNullValue())
            .body("subjectName", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("수업 상세 조회 실패(401 Unauthorized) - 인증 없음")
    void getLessonDetail_Unauthorized() {
        given()
            .when()
            .get("/{lessonId}", 1L)
            .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("교사는 타인의 수업 상세 조회 불가(404 Not Found)")
    void getLessonDetail_Volunteer_CannotAccessOthersLesson() {
        long othersLessonId = createTrackedLessonFixture(
            "read-detail-other",
            TEACHER2_ID,
            "2042-07-09",
            "WEDNESDAY",
            3,
            "2027-07-09",
            "19:20:00",
            "20:00:00",
            1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get("/{lessonId}", othersLessonId)
            .then()
            .statusCode(404)
            .log().all();
    }
}
