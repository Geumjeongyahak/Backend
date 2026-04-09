package sonmoeum.e2e.auth;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.auth.v1.dto.request.LocalLoginRequest;
import sonmoeum.domain.auth.v1.dto.response.TokenResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: 로그인 테스트")
class AuthLoginTest extends AuthBaseTest {

    @Test
    @DisplayName("올바른 아이디와 비밀번호로 로그인 성공(200 OK)")
    void login_Success() {
        LocalLoginRequest req = new LocalLoginRequest(
                TEST_ADMIN_USERNAME,
                TEST_ADMIN_PASSWORD
        );

        var response = given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .body("tokenType", equalTo("Bearer"))
            .body("accessTokenExpiresAt", notNullValue())
            .body("refreshTokenExpiresAt", notNullValue())
            .log().all()
            .extract()
            .as(TokenResponse.class);

        // 토큰이 실제로 작동하는지 확인 (basePath 임시 초기화)
        String originalBasePath = RestAssured.basePath;
        RestAssured.basePath = "";

        given()
            .header(AUTH_HEADER, getAuthHeader(response.accessToken()))
        .when()
            .get("/api/v1/users/me")
        .then()
            .statusCode(200);

        RestAssured.basePath = originalBasePath;
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패(401 Unauthorized)")
    void login_WrongPassword() {
        LocalLoginRequest req = new LocalLoginRequest(
                TEST_ADMIN_USERNAME,
                "wrongpassword123!"
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/login")
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인 실패(401 Unauthorized)")
    void login_UserNotFound() {
        LocalLoginRequest req = new LocalLoginRequest(
                "nonexistentuser1234",
                "password123!"
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/login")
        .then()
            .statusCode(401)
            .body("code", equalTo("AUTH005"))  // INVALID_CREDENTIALS (사용자 존재 여부 노출 방지)
            .log().all();
    }

    @Test
    @DisplayName("아이디 누락 시 로그인 실패(400 Bad Request)")
    void login_MissingUsername() {
        String invalidReq = """
            {
                "password": "password123!"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidReq)
        .when()
            .post("/login")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("비밀번호 누락 시 로그인 실패(400 Bad Request)")
    void login_MissingPassword() {
        String invalidReq = """
            {
                "username": "admin1234"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidReq)
        .when()
            .post("/login")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("짧은 아이디로 로그인 실패(400 Bad Request)")
    void login_ShortUsername() {
        LocalLoginRequest req = new LocalLoginRequest(
                "short",  // 8자 미만
                "password123!"
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/login")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("짧은 비밀번호로 로그인 실패(400 Bad Request)")
    void login_ShortPassword() {
        LocalLoginRequest req = new LocalLoginRequest(
                TEST_ADMIN_USERNAME,
                "short"  // 8자 미만
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/login")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("빈 JSON으로 로그인 시도 시 실패(400 Bad Request)")
    void login_EmptyBody() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/login")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("GET으로 로그인 엔드포인트 호출 시 405 Method Not Allowed와 표준 에러 응답 반환")
    void login_GetMethodNotAllowed() {
        given()
        .when()
            .get("/login")
        .then()
            .statusCode(405)
            .body("code", equalTo("VAL007"))
            .body("detail", equalTo("지원하지 않는 HTTP 메서드입니다."))
            .body("method", equalTo("GET"))
            .body("supportedMethods", hasItem("POST"))
            .log().all();
    }
}
