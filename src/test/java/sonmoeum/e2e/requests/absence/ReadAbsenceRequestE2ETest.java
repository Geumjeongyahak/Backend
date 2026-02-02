package sonmoeum.e2e.requests.absence;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;

import sonmoeum.e2e.requests.BaseRequestE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("e2e")
@DisplayName("Absence Request E2E Tests - Read")
class ReadAbsenceRequestE2ETest extends BaseRequestE2ETest {

    @Test
    @DisplayName("결석 요청 목록 조회 성공")
    void getAbsenceRequestsSuccess() {
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/v1/requests/absence")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.currentPage", greaterThan(-1));
    }

    @Test
    @DisplayName("결석 요청 조회 실패 - 권한 없음")
    void getAbsenceRequestsFailUnauthorized() {
        given()
        .when()
            .get("/api/v1/requests/absence")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
