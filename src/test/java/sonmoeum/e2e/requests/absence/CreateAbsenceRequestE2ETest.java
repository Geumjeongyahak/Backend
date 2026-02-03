package sonmoeum.e2e.requests.absence;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import sonmoeum.e2e.requests.BaseRequestE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Tag("e2e")
@DisplayName("Absence Request E2E Tests - Create")
class CreateAbsenceRequestE2ETest extends BaseRequestE2ETest {

    @Test
    @DisplayName("결석 요청 생성 성공")
    void createAbsenceRequestSuccess() {
        Long lessonId = setupLessonForRequest();
        String sessionCookie = getVolunteerSession();

        var requestBody = createAbsenceRequestBody(lessonId, "개인 사정으로 인한 결석");

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(requestBody)
        .when()
            .post("/api/v1/requests/absence")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.reason", equalTo("개인 사정으로 인한 결석"))
            .body("data.id", notNullValue());
    }

    @Test
    @DisplayName("결석 요청 생성 실패 - 인증 필요")
    void createAbsenceRequestFailUnauthenticated() {
        Long lessonId = setupLessonForRequest();

        var requestBody = createAbsenceRequestBody(lessonId, "개인 사정");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(requestBody)
        .when()
            .post("/api/v1/requests/absence")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("결석 요청 생성 실패 - 잘못된 수업 ID")
    void createAbsenceRequestFailInvalidLessonId() {
        String sessionCookie = getVolunteerSession();

        var requestBody = createAbsenceRequestBody(99999L, "개인 사정");

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(requestBody)
        .when()
            .post("/api/v1/requests/absence")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
