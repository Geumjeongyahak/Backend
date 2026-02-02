package sonmoeum.e2e.lessons;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("e2e")
@DisplayName("Lesson E2E Tests - Get My Lessons")
class GetMyLessonsE2ETest extends BaseLessonE2ETest {

    @Test
    @DisplayName("내 수업 조회 성공")
    void getMyLessonsSuccess() {
        String sessionCookie = loginAsVolunteer();

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .get("/api/v1/lessons/me")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("내 수업 조회 - 날짜 필터링")
    void getMyLessonsWithDateFilter() {
        String sessionCookie = loginAsVolunteer();

        given()
            .cookie("SESSION", sessionCookie)
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-02-28")
        .when()
            .get("/api/v1/lessons/me")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("내 수업 조회 실패 - 인증 필요")
    void getMyLessonsFailUnauthenticated() {
        given()
        .when()
            .get("/api/v1/lessons/me")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
