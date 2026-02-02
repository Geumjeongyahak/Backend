package sonmoeum.e2e.lessons;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("e2e")
@DisplayName("Lesson E2E Tests - Read")
class ReadLessonE2ETest extends BaseLessonE2ETest {

    @Test
    @DisplayName("수업 목록 조회 성공")
    void getLessonsSuccess() {
        setupSubjectAndLesson();
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/v1/lessons")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.size()", greaterThan(0))
            .body("data.totalCount", greaterThan(0));
    }

    @Test
    @DisplayName("수업 상세 조회 성공")
    void getLessonByIdSuccess() {
        Long lessonId = setupSubjectAndLesson();
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .get("/api/v1/lessons/" + lessonId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.id", greaterThan(0));
    }

    @Test
    @DisplayName("수업 조회 실패 - 권한 없음")
    void getLessonsFailUnauthorized() {
        given()
        .when()
            .get("/api/v1/lessons")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
