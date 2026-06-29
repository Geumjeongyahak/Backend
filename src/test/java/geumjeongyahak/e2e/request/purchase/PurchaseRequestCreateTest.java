package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;

/**
 * 기자재 구입 요청 생성 E2E 테스트.
 * init_data 의 CLASSROOM_ID=1 (벚꽃반)을 재사용한다.
 * 분반 상태는 구입 요청으로 변경되지 않으므로 격리 문제 없음.
 */
@Tag("purchase-request")
@DisplayName("E2E: 기자재 구입 요청 생성 테스트")
class PurchaseRequestCreateTest extends RequestBaseTest {

    private static final String APPS_SCRIPT_BOT_EMAIL = "geumjeongyahak-apps-script-bot@gmail.com";
    private static final String APPS_SCRIPT_BOT_PASSWORD = "apps-script-bot123!";

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    private Long createdRequestId;
    private final List<Long> createdRequestIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        createdRequestIds.forEach(purchaseRequestRepository::deleteById);
        createdRequestIds.clear();
        if (createdRequestId != null) {
            purchaseRequestRepository.deleteById(createdRequestId);
            createdRequestId = null;
        }
    }

    // ── 성공 ──────────────────────────────────────────────

    @Test
    @DisplayName("인증된 봉사자가 구입 요청 생성 → 201, 필드 검증")
    void createRequest_asVolunteer_returns201() {
        createdRequestId = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "교재 구입 요청"),
                entry("content", "한글 기초 교재가 필요합니다."),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "한글 기초 교재"),
                    entry("reason", "수업 교재 부족"),
                    entry("quantity", 2),
                    entry("paymentType", "PREPAID")
                )))
            ))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("classroomId", equalTo((int) CLASSROOM_ID))
            .body("title", equalTo("교재 구입 요청"))
            .body("totalPrice", equalTo(0))
            .body("status", equalTo("PENDING"))
            .body("requestedByName", equalTo("홍길동"))
            .body("items", hasSize(1))
            .body("items[0].name", equalTo("한글 기초 교재"))
            .body("items[0].quantity", equalTo(2))
            .body("items[0].paymentType", equalTo("PREPAID"))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Test
    @DisplayName("관리자가 구입 요청 생성 → 201")
    void createRequest_asAdmin_returns201() {
        createdRequestId = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "칠판 구입"),
                entry("content", "교실용 칠판"),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "칠판"),
                    entry("reason", "교실 비품 교체"),
                    entry("quantity", 1),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Test
    @DisplayName("관리자가 실제 요청자를 지정해 구입 요청 대리 생성 → 201")
    void createRequestByAdmin_withRequestedById_returns201() {
        createdRequestIds.add(given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("requestedById", TEACHER2_ID),
                entry("title", "시트 구입 요청"),
                entry("content", "Apps Script에서 등록한 구입 요청입니다."),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "프린터 토너"),
                    entry("reason", "수업 자료 출력"),
                    entry("quantity", 1),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("requestedById", equalTo((int) TEACHER2_ID))
            .body("requestedByName", equalTo("김철수"))
            .body("title", equalTo("시트 구입 요청"))
            .body("status", equalTo("PENDING"))
            .extract()
            .jsonPath()
            .getLong("id"));
    }

    @Test
    @DisplayName("purchase-request:manage:* 권한자가 구입 요청 대리 생성 → 201")
    void createRequestByAdmin_withManagePermission_returns201() {
        String manageToken = createAccessTokenWithPermission(
            "purchase-delegate",
            RoleType.VOLUNTEER,
            "purchase-request:manage:*"
        );

        createdRequestIds.add(given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(manageToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("requestedById", TEACHER_ID),
                entry("title", "권한자 대리 요청"),
                entry("content", "구입 요청 관리 권한자가 등록합니다."),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "마커"),
                    entry("reason", "수업 판서"),
                    entry("quantity", 3),
                    entry("paymentType", "PREPAID")
                )))
            ))
            .post()
            .then()
            .statusCode(201)
            .body("requestedById", equalTo((int) TEACHER_ID))
            .body("requestedByName", equalTo("홍길동"))
            .extract()
            .jsonPath()
            .getLong("id"));
    }

    @Test
    @DisplayName("Apps Script Bot이 실제 요청자를 지정해 구입 요청 대리 생성 → 201")
    void createRequestByAdmin_withAppsScriptBot_returns201() {
        String botAccessToken = loginAppsScriptBot();

        createdRequestIds.add(given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(botAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("requestedById", TEACHER2_ID),
                entry("title", "Bot 대리 구입 요청"),
                entry("content", "Apps Script Bot이 시트 값을 동기화합니다."),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "복사용지"),
                    entry("reason", "수업 자료 인쇄"),
                    entry("quantity", 2),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(201)
            .body("requestedById", equalTo((int) TEACHER2_ID))
            .body("requestedByName", equalTo("김철수"))
            .extract()
            .jsonPath()
            .getLong("id"));
    }

    @Test
    @DisplayName("권한 없는 사용자의 구입 요청 대리 생성 → 403")
    void createRequestByAdmin_withoutPermission_returns403() {
        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("requestedById", TEACHER2_ID),
                entry("title", "권한 없는 요청"),
                entry("content", "권한 없는 사용자는 대리 생성할 수 없습니다."),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "교재"),
                    entry("reason", "수업 준비"),
                    entry("quantity", 1),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 요청자 ID로 구입 요청 대리 생성 → 404")
    void createRequestByAdmin_nonExistentRequester_returns404() {
        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("requestedById", 99999L),
                entry("title", "없는 요청자"),
                entry("content", "존재하지 않는 사용자로 대리 생성할 수 없습니다."),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "교재"),
                    entry("reason", "수업 준비"),
                    entry("quantity", 1),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(404);
    }

    private String loginAppsScriptBot() {
        return given()
            .basePath("/api/v1/auth")
            .contentType(ContentType.JSON)
            .body(new LocalLoginRequest(APPS_SCRIPT_BOT_EMAIL, APPS_SCRIPT_BOT_PASSWORD))
            .post("/login")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("accessToken");
    }

    // ── 인증 오류 ─────────────────────────────────────────

    @Test
    @DisplayName("인증 없이 구입 요청 → 401")
    void createRequest_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/purchase-requests")
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "품목"),
                    entry("reason", "사유"),
                    entry("quantity", 1),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("게스트 구입 요청 생성 → 403")
    void createRequest_asGuest_returns403() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "품목"),
                    entry("reason", "사유"),
                    entry("quantity", 1),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(403);
    }

    // ── 도메인 오류 ───────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 분반 ID → 404")
    void createRequest_nonExistentClassroom_returns404() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", 99999L),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "품목"),
                    entry("reason", "사유"),
                    entry("quantity", 1),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(404);
    }

    // ── 유효성 오류 ───────────────────────────────────────

    @Test
    @DisplayName("title 이 빈 문자열 → 400")
    void createRequest_blankTitle_returns400() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", ""),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "품목"),
                    entry("reason", "사유"),
                    entry("quantity", 1),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("items 가 빈 배열 → 400")
    void createRequest_emptyItems_returns400() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of())
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("quantity 가 0 → 400")
    void createRequest_zeroQuantity_returns400() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "품목"),
                    entry("reason", "사유"),
                    entry("quantity", 0),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("item name 이 빈 문자열 → 400")
    void createRequest_blankItemName_returns400() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("items", List.of(Map.ofEntries(
                    entry("name", ""),
                    entry("reason", "사유"),
                    entry("quantity", 1),
                    entry("paymentType", "ACTUAL")
                )))
            ))
            .post()
            .then()
            .statusCode(400);
    }

}
