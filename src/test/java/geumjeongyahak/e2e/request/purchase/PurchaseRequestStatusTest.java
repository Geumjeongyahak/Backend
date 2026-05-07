package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;

/**
 * 기자재 구입 요청 승인·반려·조회 E2E 테스트.
 * 구입 요청은 승인해도 과목 상태를 변경하지 않으므로 side-effect 없음.
 */
@Tag("purchase-request")
@DisplayName("E2E: 기자재 구입 요청 승인·반려·조회 테스트")
class PurchaseRequestStatusTest extends RequestBaseTest {

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    private Long currentRequestId;

    @AfterEach
    void cleanup() {
        if (currentRequestId != null) {
            if (purchaseRequestRepository.existsById(currentRequestId)) {
                purchaseRequestRepository.deleteById(currentRequestId);
            }
            currentRequestId = null;
        }
    }

    private Long setupPendingRequest() {
        return createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "교재 구입", "교재가 필요합니다.", 20000L);
    }

    // ── 승인 (approve) ────────────────────────────────────

    @Test
    @DisplayName("관리자 구입 요청 승인 → 200, APPROVED, approvalAt 설정")
    void approve_asAdmin_returns200() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("approvalAt", notNullValue())
            .body("approvalByName", notNullValue());
    }

    @Test
    @DisplayName("이미 처리된 구입 요청 재승인 → 409")
    void approve_alreadyProcessed_returns409() {
        currentRequestId = setupPendingRequest();

        given().basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/approve", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/approve", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("봉사자 승인 시도 → 403")
    void approve_asVolunteer_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 구입 요청 승인 → 404")
    void approve_notFound_returns404() {
        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/approve", 99999L)
            .then()
            .statusCode(404);
    }

    // ── 반려 (reject) ─────────────────────────────────────

    @Test
    @DisplayName("관리자 구입 요청 반려 → 200, REJECTED, note 저장")
    void reject_asAdmin_withNote_returns200() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "예산 초과로 반려합니다."))
            .patch("/{requestId}/reject", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("REJECTED"))
            .body("note", equalTo("예산 초과로 반려합니다."));
    }

    @Test
    @DisplayName("note 없이 반려 → 400")
    void reject_withoutNote_returns400() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of())
            .patch("/{requestId}/reject", currentRequestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("이미 처리된 요청 반려 → 409")
    void reject_alreadyProcessed_returns409() {
        currentRequestId = setupPendingRequest();

        given().basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/approve", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "뒤늦은 반려"))
            .patch("/{requestId}/reject", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("봉사자 반려 시도 → 403")
    void reject_asVolunteer_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "반려"))
            .patch("/{requestId}/reject", currentRequestId)
            .then()
            .statusCode(403);
    }

    // ── 삭제 (delete) ─────────────────────────────────────

    @Test
    @DisplayName("요청 작성자가 PENDING 구입 요청 삭제 → 204")
    void delete_asOwnerAndPending_returns204() {
        currentRequestId = setupPendingRequest();
        Long requestId = currentRequestId;

        given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}", requestId)
            .then()
            .statusCode(204);

        assertThat(purchaseRequestRepository.existsById(requestId)).isFalse();
        currentRequestId = null;
    }

    @Test
    @DisplayName("타인이 구입 요청 삭제 시도 → 403")
    void delete_asOtherVolunteer_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .delete("/{requestId}", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("이미 처리된 구입 요청 삭제 시도 → 409")
    void delete_processedRequest_returns409() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}", currentRequestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("존재하지 않는 구입 요청 삭제 → 404")
    void delete_notFound_returns404() {
        given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}", 99999L)
            .then()
            .statusCode(404);
    }

    // ── 재확인 요청 (reconfirmation) ───────────────────────

    @Test
    @DisplayName("요청 작성자가 PURCHASED 구입 요청 재확인 요청 → 204")
    void requestReconfirmation_asOwnerAndPurchased_returns204() {
        currentRequestId = setupPurchasedRequest();

        given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .post("/{requestId}/reconfirmation", currentRequestId)
            .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("타인이 구입 요청 재확인 요청 시도 → 403")
    void requestReconfirmation_asOtherVolunteer_returns403() {
        currentRequestId = setupPurchasedRequest();

        given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .post("/{requestId}/reconfirmation", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("PURCHASED가 아닌 구입 요청 재확인 요청 → 409")
    void requestReconfirmation_invalidStatus_returns409() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .post("/{requestId}/reconfirmation", currentRequestId)
            .then()
            .statusCode(409);
    }

    // ── 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("관리자는 전체 목록 / 봉사자는 본인 요청만 조회")
    void getList_adminSeesAll_volunteerSeesOnlyOwn() {
        Long request1Id = createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "v1 요청", "내용1", 1000L);
        Long request2Id = createPurchaseRequest(
            getAuthHeader(volunteer2Token), CLASSROOM_ID, "v2 요청", "내용2", 2000L);

        try {
            List<Long> adminIds = given()
                .basePath("/api/v1/admin/purchase-requests")
                .header(AUTH_HEADER, getAuthHeader(adminToken))
                .get()
                .then().statusCode(200)
                .extract().jsonPath().getList("id", Long.class);
            assertThat(adminIds).contains(request1Id, request2Id);

            List<Long> v1Ids = given()
                .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
                .pathParam("classroomId", CLASSROOM_ID)
                .header(AUTH_HEADER, getAuthHeader(volunteerToken))
                .get()
                .then().statusCode(200)
                .extract().jsonPath().getList("id", Long.class);
            assertThat(v1Ids).contains(request1Id);
            assertThat(v1Ids).doesNotContain(request2Id);

        } finally {
            purchaseRequestRepository.deleteById(request1Id);
            purchaseRequestRepository.deleteById(request2Id);
        }
    }

    @Test
    @DisplayName("status=PENDING 필터 → 승인된 요청 미포함")
    void getList_filteredByApprovedStatus_excludesPending() {
        currentRequestId = setupPendingRequest();

        // APPROVED 목록에는 PENDING 요청이 없어야 함
        List<Long> approvedIds = given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("status", "APPROVED")
            .get()
            .then().statusCode(200)
            .extract().jsonPath().getList("id", Long.class);

        assertThat(approvedIds).doesNotContain(currentRequestId);
    }

    @Test
    @DisplayName("타인의 단건 조회 → 403")
    void getDetail_byNonOwner_returns403() {
        currentRequestId = createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "개인 요청", "내용", 5000L);

        given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .get("/{requestId}", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("인증 없이 목록 조회 → 401")
    void getList_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .get()
            .then()
            .statusCode(401);
    }

    private Long setupPurchasedRequest() {
        Long requestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/approve", requestId)
            .then()
            .statusCode(200);

        Long itemId = given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", requestId)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getLong("items[0].id");

        given()
            .basePath("/api/v1/classrooms/{classroomId}/purchase-requests")
            .pathParam("classroomId", CLASSROOM_ID)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("items", List.of(Map.of(
                "itemId", itemId,
                "price", 20000L
            ))))
            .post("/{requestId}/report", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("PURCHASED"));

        return requestId;
    }
}
