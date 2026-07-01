package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;

@Tag("purchase-request")
@DisplayName("E2E: 기자재 구입 요청 목록 검색 테스트")
class PurchaseRequestSearchTest extends RequestBaseTest {

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    private final List<Long> createdRequestIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        createdRequestIds.stream()
            .filter(purchaseRequestRepository::existsById)
            .forEach(purchaseRequestRepository::deleteById);
        createdRequestIds.clear();
    }

    @Test
    @DisplayName("관리자 구입 요청 목록은 제목, 분반명, 작성자명으로 검색할 수 있다")
    void adminList_canSearchByKeywordClassroomAndRequester() {
        String marker = "검색확장-" + UUID.randomUUID();
        Long requestId = createRequest(marker + "-교재");

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("keyword", marker)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content.id", hasItem(requestId.intValue()));

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("classroomName", "벚꽃")
            .queryParam("keyword", marker)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content.id", hasItem(requestId.intValue()));

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("requestedByName", "홍길동")
            .queryParam("keyword", marker)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content.id", hasItem(requestId.intValue()));
    }

    @Test
    @DisplayName("일반 구입 요청 목록은 mine=true와 검색 조건을 함께 적용한다")
    void volunteerList_appliesMineAndSearchTogether() {
        String marker = "내요청검색-" + UUID.randomUUID();
        Long myRequestId = createRequest(marker + "-내요청");
        Long otherRequestId = createPurchaseRequest(
            getAuthHeader(volunteer2Token),
            CLASSROOM_ID,
            marker + "-타인요청",
            "검색 조건 격리 확인",
            10000L
        );
        createdRequestIds.add(otherRequestId);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .queryParam("mine", true)
            .queryParam("keyword", marker)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content.id", hasItem(myRequestId.intValue()))
            .body("content.requestedById", everyItem(org.hamcrest.Matchers.equalTo((int) TEACHER_ID)));
    }

    private Long createRequest(String title) {
        Long requestId = createPurchaseRequest(
            getAuthHeader(volunteerToken),
            CLASSROOM_ID,
            title,
            "검색 조건 확인",
            10000L
        );
        createdRequestIds.add(requestId);
        return requestId;
    }
}
