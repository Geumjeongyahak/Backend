package geumjeongyahak.e2e.common;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("common")
@DisplayName("E2E: 요청 바인딩 오류 공통 처리")
class RequestBindingErrorTest extends BaseE2ETest {

    private static final String VOLUNTEER = "binding-volunteer@test.com";

    private String adminToken;
    private String volunteerToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        userTestHelper.createTestUser(VOLUNTEER, "바인딩 테스트 봉사자", "password", RoleType.VOLUNTEER);

        adminToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_EMAIL);
        volunteerToken = userTestHelper.generateAccessTokenByUserKey(VOLUNTEER);
    }

    @Test
    @DisplayName("@ModelAttribute 날짜 변환 실패는 400을 반환한다")
    void modelAttributeDateConversionError_returns400() {
        given()
            .queryParam("from", "not-a-date")
            .queryParam("to", "2026-06-03")
        .when()
            .get("/api/v1/lessons")
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"));
    }

    @Test
    @DisplayName("필수 요청 파라미터 누락은 400을 반환한다")
    void missingRequestParameter_returns400() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .queryParam("classroomId", 1)
        .when()
            .get("/api/v1/daily-schedules/detail")
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL003"))
            .body("field", equalTo("lessonDate"));
    }

    @Test
    @DisplayName("요청 파라미터 타입 변환 실패는 400을 반환한다")
    void requestParameterTypeMismatch_returns400() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .queryParam("classroomId", "not-a-number")
            .queryParam("lessonDate", "2026-06-03")
        .when()
            .get("/api/v1/daily-schedules/detail")
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL002"))
            .body("parameter", equalTo("classroomId"));
    }

    @Test
    @DisplayName("관리자 API enum 목록 요청 바인딩 실패는 400을 반환한다")
    void adminEnumRequestParameterTypeMismatch_returns400() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("status", "invalid")
        .when()
            .get("/api/v1/admin/purchase-requests")
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))
            .body("errors[0].field", equalTo("status"));
    }

    @Test
    @DisplayName("경로 변수 타입 변환 실패는 400을 반환한다")
    void pathVariableTypeMismatch_returns400() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
        .when()
            .get("/api/v1/admin/teacher-applications/not-a-number")
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL002"))
            .body("parameter", equalTo("applicationId"));
    }

    @Test
    @DisplayName("요청 본문 파싱 실패는 400을 반환한다")
    void requestBodyParseError_returns400() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body("""
                {
                  "status": "invalid"
                }
                """)
        .when()
            .patch("/api/v1/lessons/1/status")
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL002"));
    }
}
