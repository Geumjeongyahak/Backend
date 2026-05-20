package geumjeongyahak.e2e.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import geumjeongyahak.e2e.BaseE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class CorsPreflightTest extends BaseE2ETest {

    @Test
    @DisplayName("인증이 필요한 API도 CORS preflight 요청은 토큰 없이 허용한다")
    void preflightRequestForProtectedApiIsAllowedWithoutAuthentication() {
        given()
            .header(HttpHeaders.ORIGIN, "https://geumjeongschool.com")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
        .when()
            .options("/api/v1/users")
        .then()
            .statusCode(HttpStatus.OK.value())
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://geumjeongschool.com")
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString(HttpMethod.POST.name()));
    }
}
