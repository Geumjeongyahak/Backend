package geumjeongyahak.e2e.common;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("E2E: 로그 요청 정보 테스트")
class RequestLogContextTest extends RequestBaseTest {

    @Test
    @DisplayName("권한 없는 API 호출의 경고 로그에 요청과 사용자 ID 가 실린다")
    void accessDenied_logHasRequestAndUser(CapturedOutput output) {
        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "권한 없는 승인"))
            .patch("/{requestId}/approve", 999999L)
            .then()
            .statusCode(403);

        assertThat(logLine(output, "AccessDeniedException"))
            .contains("\"request\":\"PATCH /api/v1/admin/purchase-requests/999999/approve\"")
            .contains("\"user_id\":\"" + TEACHER_ID + "\"")
            .contains("\"trace_id\":\"");
    }

    @Test
    @DisplayName("로그인하지 않은 요청의 오류 로그에는 사용자 ID 없이 요청만 실린다")
    void anonymous_logHasRequestWithoutUser(CapturedOutput output) {
        given()
            .basePath("/api/v1/auth")
            .contentType(ContentType.JSON)
            .body(new LocalLoginRequest(TEST_ADMIN_EMAIL, "wrong-password"))
            .post("/login")
            .then()
            .statusCode(401);

        assertThat(logLine(output, "BadCredentialsException"))
            .contains("\"request\":\"POST /api/v1/auth/login\"")
            .doesNotContain("\"user_id\"");
    }

    private String logLine(CapturedOutput output, String keyword) {
        return output.getOut().lines()
            .filter(line -> line.contains(keyword))
            .reduce((first, second) -> second)
            .orElse("");
    }
}
