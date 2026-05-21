package geumjeongyahak.e2e.auth;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.v1.dto.request.LocalSignupRequest;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: 회원가입 테스트")
class AuthSignupTest extends AuthBaseTest {

    @Test
    @DisplayName("올바른 정보로 회원가입 성공(201 Created)")
    void signup_Success() {
        String uniqueEmail = "signuptest" + System.currentTimeMillis() + "@test.com";
        LocalSignupRequest req = new LocalSignupRequest(
                "password123!",
                "signup-test",
                "회원가입 테스트",
                uniqueEmail,
                null,
                "010-1234-5678",
                "900101"
        );

        var response = given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(201)
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .body("tokenType", equalTo("Bearer"))
            .body("accessTokenExpiresAt", notNullValue())
            .body("refreshTokenExpiresAt", notNullValue())
            .log().all()
            .extract()
            .as(TokenResponse.class);

        // 발급받은 토큰이 작동하는지 확인 (basePath 임시 초기화)
        String originalBasePath = RestAssured.basePath;
        RestAssured.basePath = "";

        given()
            .header(AUTH_HEADER, getAuthHeader(response.accessToken()))
        .when()
            .get("/api/v1/users/me")
        .then()
            .statusCode(200)
            .body("name", equalTo("회원가입 테스트"));

        RestAssured.basePath = originalBasePath;
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 실패(409 Conflict)")
    void signup_DuplicateEmail() {
        LocalSignupRequest req = new LocalSignupRequest(
                "password123!",
                "duplicate-test",
                "중복 테스트",
                TEST_ADMIN_EMAIL,  // 이미 존재하는 이메일
                null,
                "010-1234-5678",
                "900101"
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(409)
            .log().all();
    }

    @Test
    @DisplayName("필수 필드 누락 시 회원가입 실패(400 Bad Request)")
    void signup_MissingRequiredFields() {
        String invalidReq = """
            {
                "phoneNumber": "010-1234-5678"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidReq)
        .when()
            .post("/signup")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("짧은 비밀번호로 회원가입 실패(400 Bad Request)")
    void signup_ShortPassword() {
        LocalSignupRequest req = new LocalSignupRequest(
                "short",  // 8자 미만
                "short-password-test",
                "테스트 사용자",
                "test" + System.currentTimeMillis() + "@test.com",
                null,
                "010-1234-5678",
                "900101"
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("잘못된 이메일 형식으로 회원가입 실패(400 Bad Request)")
    void signup_InvalidEmail() {
        LocalSignupRequest req = new LocalSignupRequest(
                "password123!",
                "invalid-email-test",
                "이메일 테스트",
                "invalid-email",  // 잘못된 이메일 형식
                null,
                "010-1234-5678",
                "900101"
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("잘못된 전화번호 형식으로 회원가입 실패(400 Bad Request)")
    void signup_InvalidPhoneNumber() {
        String uniqueEmail = "phonetest" + System.currentTimeMillis() + "@test.com";
        LocalSignupRequest req = new LocalSignupRequest(
                "password123!",
                "invalid-phone-test",
                "전화번호 테스트",
                uniqueEmail,
                null,
                "invalid-phone",  // 잘못된 전화번호 형식
                "900101"
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("긴 이름으로 회원가입 실패(400 Bad Request)")
    void signup_TooLongName() {
        String uniqueEmail = "longname" + System.currentTimeMillis() + "@test.com";
        LocalSignupRequest req = new LocalSignupRequest(
                "password123!",
                "long-name-test",
                "a".repeat(51),  // 50자 초과
                uniqueEmail,
                null,
                "010-1234-5678",
                "900101"
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("전화번호 없이 회원가입 성공(201 Created) - 선택 필드")
    void signup_WithoutPhoneNumber() {
        String uniqueEmail = "minimal" + System.currentTimeMillis() + "@test.com";
        LocalSignupRequest req = new LocalSignupRequest(
                "password123!",
                "minimal-test",
                "최소 정보",
                uniqueEmail,
                null,
                null,   // 전화번호 선택
                "900101"
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(201)
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .log().all()
            .extract()
            .as(TokenResponse.class);
    }
}
