package sonmoeum.e2e.auth;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.auth.v1.dto.request.LocalSignupRequest;
import sonmoeum.domain.auth.v1.dto.response.TokenResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: 회원가입 테스트")
class AuthSignupTest extends AuthBaseTest {

    @Test
    @DisplayName("올바른 정보로 회원가입 성공(201 Created)")
    void signup_Success() {
        String uniqueUsername = "signuptest" + System.currentTimeMillis();
        LocalSignupRequest req = new LocalSignupRequest(
                uniqueUsername,
                "password123!",
                "회원가입 테스트",
                uniqueUsername + "@test.com",
                "010-1234-5678"
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

        userTestHelper.setUser(uniqueUsername);

        // 발급받은 토큰이 작동하는지 확인 (basePath 임시 초기화)
        String originalBasePath = RestAssured.basePath;
        RestAssured.basePath = "";

        given()
            .header(AUTH_HEADER, getAuthHeader(response.accessToken()))
        .when()
            .get("/api/v1/users/me")
        .then()
            .statusCode(200)
            .body("username", equalTo(uniqueUsername));

        RestAssured.basePath = originalBasePath;
    }

    @Test
    @DisplayName("중복된 아이디로 회원가입 실패(409 Conflict)")
    void signup_DuplicateUsername() {
        LocalSignupRequest req = new LocalSignupRequest(
                TEST_ADMIN_USERNAME,  // 이미 존재하는 사용자
                "password123!",
                "중복 테스트",
                "duplicate@test.com",
                "010-1234-5678"
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
                "username": "testuser1234"
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
    @DisplayName("짧은 아이디로 회원가입 실패(400 Bad Request)")
    void signup_ShortUsername() {
        LocalSignupRequest req = new LocalSignupRequest(
                "short",  // 8자 미만
                "password123!",
                "테스트 사용자",
                "test@test.com",
                "010-1234-5678"
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
    @DisplayName("짧은 비밀번호로 회원가입 실패(400 Bad Request)")
    void signup_ShortPassword() {
        LocalSignupRequest req = new LocalSignupRequest(
                "testuser1234",
                "short",  // 8자 미만
                "테스트 사용자",
                "test@test.com",
                "010-1234-5678"
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
        String uniqueUsername = "emailtest" + System.currentTimeMillis();
        LocalSignupRequest req = new LocalSignupRequest(
                uniqueUsername,
                "password123!",
                "이메일 테스트",
                "invalid-email",  // 잘못된 이메일 형식
                "010-1234-5678"
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
        String uniqueUsername = "phonetest" + System.currentTimeMillis();
        LocalSignupRequest req = new LocalSignupRequest(
                uniqueUsername,
                "password123!",
                "전화번호 테스트",
                uniqueUsername + "@test.com",
                "invalid-phone"  // 잘못된 전화번호 형식
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
        String uniqueUsername = "longname" + System.currentTimeMillis();
        LocalSignupRequest req = new LocalSignupRequest(
                uniqueUsername,
                "password123!",
                "a".repeat(51),  // 50자 초과
                uniqueUsername + "@test.com",
                "010-1234-5678"
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
    @DisplayName("선택 필드 없이 회원가입 성공(201 Created)")
    void signup_WithoutOptionalFields() {
        String uniqueUsername = "minimal" + System.currentTimeMillis();
        LocalSignupRequest req = new LocalSignupRequest(
                uniqueUsername,
                "password123!",
                "최소 정보",
                null,  // 이메일 선택
                null   // 전화번호 선택
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

        userTestHelper.setUser(uniqueUsername);
    }
}
