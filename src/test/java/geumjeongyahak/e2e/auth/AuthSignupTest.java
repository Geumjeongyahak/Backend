package geumjeongyahak.e2e.auth;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.common.security.jwt.JwtTokenProvider;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.auth.v1.dto.request.LocalSignupRequest;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.e2e.TestStorageConfig.ControlledMailSenderService;
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: 회원가입 테스트")
class AuthSignupTest extends AuthBaseTest {

    @Autowired
    private ControlledMailSenderService mailSenderService;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("회원가입 후 이메일 인증을 완료해야 로그인할 수 있다")
    void signup_Success() {
        String uniqueEmail = "signuptest" + System.currentTimeMillis() + "@test.com";
        LocalSignupRequest req = new LocalSignupRequest(
                "password123!",
                "회원가입 테스트",
                uniqueEmail,
                "010-1234-5678",
                LocalDate.of(1990, 1, 1)
        );

        var response = given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(201)
            .body("message", equalTo("회원가입이 완료되었습니다. 이메일 인증 후 로그인해 주세요."))
            .body("accessToken", nullValue())
            .body("refreshToken", nullValue())
            .log().all()
            .extract();

        assertThat(userTestHelper.getUser(uniqueEmail).getResidentRegistrationNumberPrefix())
            .isEqualTo("900101");
        var credential = credentialRepository.findByCredentialEmailAndProvider(uniqueEmail, ProviderType.LOCAL)
            .orElseThrow();
        assertThat(credential.isEmailVerified()).isFalse();
        assertThat(credential.getEmailVerificationTokenHash()).isNotBlank();
        assertThat(mailSenderService.getLastEmailVerificationRecipient()).isEqualTo(uniqueEmail);
        String verificationCode = mailSenderService.getLastEmailVerificationCode();
        assertThat(verificationCode).isNotBlank();

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "email": "%s",
                  "password": "password123!"
                }
                """.formatted(uniqueEmail))
        .when()
            .post("/login")
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTH013"))
            .body("detail", equalTo("이메일 인증 후 로그인할 수 있습니다."));

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "email": "%s",
                  "verificationCode": "%s"
                }
                """.formatted(uniqueEmail, verificationCode))
        .when()
            .post("/email-verification/confirm")
        .then()
            .statusCode(200)
            .body("message", equalTo("이메일 인증이 완료되었습니다. 로그인해 주세요."));

        var verifiedCredential = credentialRepository.findByCredentialEmailAndProvider(uniqueEmail, ProviderType.LOCAL)
            .orElseThrow();
        assertThat(verifiedCredential.isEmailVerified()).isTrue();
        assertThat(verifiedCredential.getEmailVerificationTokenHash()).isNull();

        var loginResponse = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "email": "%s",
                  "password": "password123!"
                }
                """.formatted(uniqueEmail))
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .body("tokenType", equalTo("Bearer"))
            .extract()
            .as(TokenResponse.class);

        String originalBasePath = RestAssured.basePath;
        RestAssured.basePath = "";

        given()
            .header(AUTH_HEADER, getAuthHeader(loginResponse.accessToken()))
        .when()
            .get("/api/v1/users/me")
        .then()
            .statusCode(200)
            .body("name", equalTo("회원가입 테스트"));

        RestAssured.basePath = originalBasePath;
    }

    @Test
    @DisplayName("Google 회원가입 후 발급된 accessToken으로 내 정보를 조회할 수 있다")
    void googleSignup_accessTokenCanReadMe() {
        String uniqueEmail = "google-signup" + System.currentTimeMillis() + "@test.com";
        String tempToken = jwtTokenProvider.createOAuth2TempToken(
            "google-sub-" + System.currentTimeMillis(),
            uniqueEmail,
            "https://example.com/profile.png"
        );

        TokenResponse tokenResponse = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "tempToken": "%s",
                  "name": "구글 가입",
                  "phoneNumber": "010-1234-5678",
                  "birthDate": "1990-01-01"
                }
                """.formatted(tempToken))
        .when()
            .post("/google/signup")
        .then()
            .statusCode(201)
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .extract()
            .as(TokenResponse.class);

        String originalBasePath = RestAssured.basePath;
        RestAssured.basePath = "";

        given()
            .header(AUTH_HEADER, getAuthHeader(tokenResponse.accessToken()))
        .when()
            .get("/api/v1/users/me")
        .then()
            .statusCode(200)
            .body("email", equalTo(uniqueEmail))
            .body("name", equalTo("구글 가입"));

        RestAssured.basePath = originalBasePath;
    }

    @Test
    @DisplayName("메일 인증 링크는 인증 후 프론트 결과 페이지로 리다이렉트한다")
    void emailVerification_LinkRedirectsToFrontendResultPage() {
        String uniqueEmail = "verify-link" + System.currentTimeMillis() + "@test.com";
        LocalSignupRequest req = new LocalSignupRequest(
            "password123!",
            "메일 링크 인증",
            uniqueEmail,
            "010-1234-5678",
            LocalDate.of(1990, 1, 1)
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(201);

        String verificationCode = mailSenderService.getLastEmailVerificationCode();

        given()
            .redirects().follow(false)
            .queryParam("email", uniqueEmail)
            .queryParam("code", verificationCode)
        .when()
            .get("/email-verification/confirm")
        .then()
            .statusCode(302)
            .header("Location", startsWith("https://geumjeongschool.com/auth/email-verification?status=success"));

        var verifiedCredential = credentialRepository.findByCredentialEmailAndProvider(uniqueEmail, ProviderType.LOCAL)
            .orElseThrow();
        assertThat(verifiedCredential.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("프론트 인증 경로가 백엔드로 들어와도 인증 API로 리다이렉트한다")
    void emailVerification_FrontendPathOnApiRedirectsToVerificationEndpoint() {
        String uniqueEmail = "verify-api-host" + System.currentTimeMillis() + "@test.com";
        LocalSignupRequest req = new LocalSignupRequest(
            "password123!",
            "API 호스트 메일 링크",
            uniqueEmail,
            "010-1234-5678",
            LocalDate.of(1990, 1, 1)
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(201);

        String verificationCode = mailSenderService.getLastEmailVerificationCode();
        String originalBasePath = RestAssured.basePath;
        RestAssured.basePath = "";

        given()
            .redirects().follow(false)
            .queryParam("email", uniqueEmail)
            .queryParam("code", verificationCode)
        .when()
            .get("/auth/email-verification")
        .then()
            .statusCode(302)
            .header("Location", containsString("/api/v1/auth/email-verification/confirm?"));

        RestAssured.basePath = originalBasePath;
    }

    @Test
    @DisplayName("메일 인증 링크 실패도 프론트 결과 페이지로 리다이렉트한다")
    void emailVerification_InvalidLinkRedirectsToFrontendResultPage() {
        String uniqueEmail = "verify-fail" + System.currentTimeMillis() + "@test.com";
        LocalSignupRequest req = new LocalSignupRequest(
            "password123!",
            "메일 링크 실패",
            uniqueEmail,
            "010-1234-5678",
            LocalDate.of(1990, 1, 1)
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(201);

        given()
            .redirects().follow(false)
            .queryParam("email", uniqueEmail)
            .queryParam("code", "000000")
        .when()
            .get("/email-verification/confirm")
        .then()
            .statusCode(302)
            .header("Location", startsWith("https://geumjeongschool.com/auth/email-verification?status=invalid"));
    }

    @Test
    @DisplayName("Google 계정 이메일로 Local 회원가입하면 거절한다")
    void signup_WithExistingGoogleEmailIsRejected() {
        String uniqueEmail = "google-to-local" + System.currentTimeMillis() + "@test.com";
        String tempToken = jwtTokenProvider.createOAuth2TempToken(
            "google-sub-local-" + System.currentTimeMillis(),
            uniqueEmail,
            "https://example.com/profile.png"
        );

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "tempToken": "%s",
                  "name": "구글 기존 사용자",
                  "phoneNumber": "010-1234-5678",
                  "birthDate": "1990-01-01"
                }
                """.formatted(tempToken))
        .when()
            .post("/google/signup")
        .then()
            .statusCode(201);

        Long googleUserId = userTestHelper.getUser(uniqueEmail).getId();

        LocalSignupRequest localReq = new LocalSignupRequest(
            "password123!",
            "로컬 가입 요청 이름",
            uniqueEmail,
            "010-9999-8888",
            LocalDate.of(2000, 1, 1)
        );

        given()
            .contentType(ContentType.JSON)
            .body(localReq)
        .when()
            .post("/signup")
        .then()
            .statusCode(409)
            .body("code", equalTo("AUTH007"))
            .body("detail", equalTo("이미 Google 계정으로 가입된 이메일입니다. Google로 로그인해 주세요."));

        assertThat(credentialRepository.findByCredentialEmailAndProvider(uniqueEmail, ProviderType.LOCAL))
            .isEmpty();
        assertThat(userTestHelper.getUser(uniqueEmail).getId()).isEqualTo(googleUserId);
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 실패(409 Conflict)")
    void signup_DuplicateEmail() {
        LocalSignupRequest req = new LocalSignupRequest(
                "password123!",
                "중복 테스트",
                TEST_ADMIN_EMAIL,  // 이미 존재하는 이메일
                "010-1234-5678",
                LocalDate.of(1990, 1, 1)
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
    @DisplayName("비활성화된 Local 사용자 이메일은 재가입할 수 있다")
    void signup_DeactivatedLocalUserEmailCanSignupAgain() {
        String uniqueEmail = "resignup" + System.currentTimeMillis() + "@test.com";
        LocalSignupRequest firstReq = new LocalSignupRequest(
            "password123!",
            "첫 가입",
            uniqueEmail,
            "010-1234-5678",
            LocalDate.of(1990, 1, 1)
        );

        given()
            .contentType(ContentType.JSON)
            .body(firstReq)
        .when()
            .post("/signup")
        .then()
            .statusCode(201);

        var deactivatedUser = userRepository.findByEmail(uniqueEmail).orElseThrow();
        deactivatedUser.softDelete();
        userRepository.save(deactivatedUser);

        LocalSignupRequest secondReq = new LocalSignupRequest(
            "newpassword123!",
            "재가입",
            uniqueEmail,
            "010-9999-8888",
            LocalDate.of(2000, 1, 1)
        );

        given()
            .contentType(ContentType.JSON)
            .body(secondReq)
        .when()
            .post("/signup")
        .then()
            .statusCode(201)
            .body("message", equalTo("회원가입이 완료되었습니다. 이메일 인증 후 로그인해 주세요."));

        var reactivatedUser = userRepository.findByEmail(uniqueEmail).orElseThrow();
        assertThat(reactivatedUser.getId()).isEqualTo(deactivatedUser.getId());
        assertThat(reactivatedUser.isDeleted()).isFalse();
        assertThat(reactivatedUser.getName()).isEqualTo("재가입");
        assertThat(reactivatedUser.getPhoneNumber()).isEqualTo("010-9999-8888");
        assertThat(reactivatedUser.getRole()).isEqualTo(RoleType.GUEST);

        var credential = credentialRepository.findByCredentialEmailAndProvider(uniqueEmail, ProviderType.LOCAL)
            .orElseThrow();
        assertThat(credential.isEmailVerified()).isFalse();
        assertThat(credential.getEmailVerificationTokenHash()).isNotBlank();
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
                "테스트 사용자",
                "test" + System.currentTimeMillis() + "@test.com",
                "010-1234-5678",
                LocalDate.of(1990, 1, 1)
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
                "이메일 테스트",
                "invalid-email",  // 잘못된 이메일 형식
                "010-1234-5678",
                LocalDate.of(1990, 1, 1)
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
                "전화번호 테스트",
                uniqueEmail,
                "invalid-phone",  // 잘못된 전화번호 형식
                LocalDate.of(1990, 1, 1)
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
                "a".repeat(51),  // 50자 초과
                uniqueEmail,
                "010-1234-5678",
                LocalDate.of(1990, 1, 1)
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
                "최소 정보",
                uniqueEmail,
                null,   // 전화번호 선택
                LocalDate.of(1990, 1, 1)
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(201)
            .body("message", equalTo("회원가입이 완료되었습니다. 이메일 인증 후 로그인해 주세요."))
            .body("accessToken", nullValue())
            .body("refreshToken", nullValue())
            .log().all()
            .extract();
    }

    @Test
    @DisplayName("이메일 인증 재발송은 계정 존재 여부를 노출하지 않는다")
    void emailVerification_ResendUnknownEmailStillReturnsOk() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "email": "unknown-verification@test.com"
                }
                """)
        .when()
            .post("/email-verification/resend")
        .then()
            .statusCode(200)
            .body("message", equalTo("인증 메일이 발송되었습니다. 메일이 도착하지 않으면 입력한 이메일을 확인해 주세요."));
    }

    @Test
    @DisplayName("미래 생년월일로 회원가입 실패(400 Bad Request)")
    void signup_FutureBirthDate() {
        LocalSignupRequest req = new LocalSignupRequest(
            "password123!",
            "미래 생년월일",
            "futurebirth" + System.currentTimeMillis() + "@test.com",
            "010-1234-5678",
            LocalDate.now().plusDays(1)
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(400)
            .body("errors.message", hasItem("생년월일은 미래일 수 없습니다."));
    }

    @Test
    @DisplayName("만 100세 이상 생년월일로 회원가입 실패(400 Bad Request)")
    void signup_AgeLimitExceeded() {
        LocalSignupRequest req = new LocalSignupRequest(
            "password123!",
            "연령 제한",
            "ageboundary" + System.currentTimeMillis() + "@test.com",
            "010-1234-5678",
            LocalDate.now().minusYears(100)
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/signup")
        .then()
            .statusCode(400)
            .body("errors.message", hasItem("생년월일은 만 100세 미만이어야 합니다."));
    }
}
