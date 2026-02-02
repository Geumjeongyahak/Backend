package sonmoeum.e2e.auth;

import java.util.Map;

import sonmoeum.e2e.AbstractE2ETest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;

public abstract class BaseAuthE2ETest extends AbstractE2ETest {

    protected String performLogin(String email, String password) {
        io.restassured.response.Response response = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response();

        String sessionCookie = response.getCookie("SESSION");
        if (sessionCookie == null) {
            throw new IllegalStateException("로그인 후 SESSION 쿠키를 찾을 수 없습니다.");
        }
        return sessionCookie;
    }

    protected String getAuthenticatedSession() {
        String email = "authenticated@example.com";
        String password = "password123";
        createTestUser(email, password);
        return performLogin(email, password);
    }
}
