package geumjeongyahak.e2e.util;

import static io.restassured.RestAssured.given;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public final class AdminSessionHelper {

    private static final Pattern CSRF_PATTERN = Pattern.compile(
        "name=\"_csrf\"\\s+value=\"([^\"]+)\"|value=\"([^\"]+)\"\\s+name=\"_csrf\""
    );

    private AdminSessionHelper() {
    }

    public static AdminSession login(String username, String password) {
        Response loginPage = given()
            .basePath("")
        .when()
            .get("/admin/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .response();

        String csrfToken = extractCsrfToken(loginPage.asString());
        String initialSessionId = loginPage.cookie("JSESSIONID");

        RequestSpecification loginRequest = given()
            .basePath("")
            .contentType(ContentType.URLENC)
            .formParam("username", username)
            .formParam("password", password)
            .formParam("_csrf", csrfToken)
            .redirects()
            .follow(false);
        if (initialSessionId != null) {
            loginRequest.cookie("JSESSIONID", initialSessionId);
        }

        Response login = loginRequest
        .when()
            .post("/admin/auth/login")
        .then()
            .statusCode(302)
            .extract()
            .response();

        String sessionId = login.cookie("JSESSIONID");
        sessionId = sessionId != null ? sessionId : initialSessionId;

        Response authenticatedLoginPage = given()
            .basePath("")
            .cookie("JSESSIONID", sessionId)
        .when()
            .get("/admin/auth/login")
        .then()
            .extract()
            .response();

        String authenticatedCsrfToken = extractCsrfTokenOrNull(authenticatedLoginPage.asString());
        return new AdminSession(
            sessionId,
            authenticatedCsrfToken != null ? authenticatedCsrfToken : csrfToken
        );
    }

    private static String extractCsrfToken(String html) {
        String token = extractCsrfTokenOrNull(html);
        if (token == null) {
            throw new IllegalStateException("Admin login page does not contain a CSRF token");
        }
        return token;
    }

    private static String extractCsrfTokenOrNull(String html) {
        Matcher matcher = CSRF_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    public record AdminSession(String sessionId, String csrfToken) {
    }
}
