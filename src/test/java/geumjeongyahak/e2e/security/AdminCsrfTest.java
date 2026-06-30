package geumjeongyahak.e2e.security;

import static io.restassured.RestAssured.given;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookies;
import io.restassured.response.Response;

@Tag("security")
@DisplayName("E2E: 관리자 CSRF 보호")
class AdminCsrfTest extends BaseE2ETest {

    private static final Pattern CSRF_INPUT_PATTERN = Pattern.compile(
        "name=\"_csrf\"\\s+value=\"([^\"]+)\"|value=\"([^\"]+)\"\\s+name=\"_csrf\""
    );

    @Test
    @DisplayName("관리자 세션 POST는 CSRF 토큰이 없으면 거부된다")
    void adminPost_withoutCsrf_forbidden() {
        Cookies cookies = loginAdmin();

        given()
            .cookies(cookies)
            .contentType(ContentType.URLENC)
            .formParam("name", "csrf-blocked-classroom")
            .formParam("type", "WEEKDAY")
        .when()
            .post("/admin/classroom/classrooms")
        .then()
            .statusCode(403);
    }

    private Cookies loginAdmin() {
        Response loginPage = given()
            .redirects().follow(false)
        .when()
            .get("/admin/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .response();

        String csrf = extractCsrfToken(loginPage.asString());
        var request = given()
            .cookies(loginPage.detailedCookies())
            .redirects().follow(false)
            .contentType(ContentType.URLENC)
            .formParam("username", TEST_ADMIN_EMAIL)
            .formParam("password", TEST_ADMIN_PASSWORD);
        if (csrf != null) {
            request.formParam("_csrf", csrf);
        }

        return request
        .when()
            .post("/admin/auth/login")
        .then()
            .statusCode(302)
            .extract()
            .response()
            .detailedCookies();
    }

    private String extractCsrfToken(String html) {
        var matcher = CSRF_INPUT_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }
}
