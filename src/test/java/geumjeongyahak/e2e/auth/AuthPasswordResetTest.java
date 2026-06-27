package geumjeongyahak.e2e.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.e2e.TestStorageConfig.ControlledMailSenderService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("E2E: 비밀번호 재설정 테스트")
class AuthPasswordResetTest extends AuthBaseTest {

    @Autowired
    private ControlledMailSenderService mailSenderService;

    @Test
    @DisplayName("비밀번호를 모르는 사용자가 메일 인증번호로 비밀번호를 재설정하고 로그인한다")
    void passwordReset_Success() {
        String email = "password-reset" + System.currentTimeMillis() + "@test.com";
        userTestHelper.createTestUser(email, "비밀번호 재설정 사용자", "oldpassword123!", RoleType.GUEST);

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "email": "%s"
                }
                """.formatted(email))
        .when()
            .post("/password-reset/request")
        .then()
            .statusCode(200)
            .body("message", notNullValue());

        assertThat(mailSenderService.getLastPasswordResetRecipient()).isEqualTo(email);
        String resetCode = mailSenderService.getLastPasswordResetCode();
        assertThat(resetCode).isNotBlank();

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "email": "%s",
                  "resetCode": "%s",
                  "newPassword": "newpassword123!"
                }
                """.formatted(email, resetCode))
        .when()
            .post("/password-reset/confirm")
        .then()
            .statusCode(200)
            .body("message", equalTo("비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해 주세요."));

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "email": "%s",
                  "password": "newpassword123!"
                }
                """.formatted(email))
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 재설정을 요청해도 계정 존재 여부를 노출하지 않는다")
    void passwordReset_RequestUnknownEmailStillReturnsOk() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "email": "unknown-reset@test.com"
                }
                """)
        .when()
            .post("/password-reset/request")
        .then()
            .statusCode(200)
            .body("message", notNullValue());
    }
}
