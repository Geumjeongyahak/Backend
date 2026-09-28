package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.notification.event.RequestReviewedPushEvent;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.vendor.repository.VendorBalanceHistoryRepository;
import geumjeongyahak.domain.vendor.repository.VendorRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;

/**
 * 같은 구입 요청에 대한 상태 전이가 겹칠 때 한 번만 처리되는지 검증한다.
 */
@Tag("purchase-request")
@DisplayName("E2E: 기자재 구입 요청 상태 전이 동시 요청 테스트")
class PurchaseRequestConcurrencyTest extends RequestBaseTest {

    private static final String ADMIN_PATH = "/api/v1/admin/purchase-requests";
    private static final String USER_PATH = "/api/v1/purchase-requests";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TestConfiguration
    static class ReviewedEventConfig {

        @Bean
        ReviewedEventRecorder reviewedEventRecorder() {
            return new ReviewedEventRecorder();
        }
    }

    static class ReviewedEventRecorder {

        private final List<Long> reviewedRequestIds = new CopyOnWriteArrayList<>();

        @EventListener
        void record(RequestReviewedPushEvent event) {
            reviewedRequestIds.add(event.getRequestId());
        }

        long countOf(Long requestId) {
            return reviewedRequestIds.stream().filter(requestId::equals).count();
        }
    }

    @Autowired
    private ReviewedEventRecorder reviewedEventRecorder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorBalanceHistoryRepository vendorBalanceHistoryRepository;

    @Autowired
    private FileRepository fileRepository;

    private final List<Long> requestIds = new ArrayList<>();
    private final List<Long> vendorIds = new ArrayList<>();
    private final List<UUID> fileIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        vendorIds.forEach(vendorBalanceHistoryRepository::deleteAllByVendor_Id);
        requestIds.stream().filter(purchaseRequestRepository::existsById).forEach(purchaseRequestRepository::deleteById);
        fileIds.stream().filter(fileRepository::existsById).forEach(fileRepository::deleteById);
        vendorIds.stream().filter(vendorRepository::existsById).forEach(vendorRepository::deleteById);
        requestIds.clear();
        vendorIds.clear();
        fileIds.clear();
    }

    @Test
    @DisplayName("실 결제 결재 확인이 겹치면 하나만 성공하고 거래처 잔액은 한 번만 차감된다")
    void confirm_actualConcurrently_deductsOnce() throws Exception {
        Long vendorId = createVendorAndCharge(100000L);
        Long requestId = setupPurchasedActualRequest(vendorId, 20000L);

        List<Integer> statusCodes = callConcurrently(
            () -> confirm(requestId),
            () -> confirm(requestId)
        );

        assertProcessedOnce(statusCodes, vendorId, 80000L, requestId);
    }

    @Test
    @DisplayName("선금 결제 결재 확인이 겹치면 하나만 성공하고 거래처 잔액은 한 번만 충전된다")
    void confirm_prepaidConcurrently_chargesOnce() throws Exception {
        Long vendorId = createVendorAndCharge(100000L);
        Long requestId = setupPurchasedPrepaidRequest(vendorId, 20000L);

        List<Integer> statusCodes = callConcurrently(
            () -> confirm(requestId),
            () -> confirm(requestId)
        );

        assertProcessedOnce(statusCodes, vendorId, 120000L, requestId);
    }

    @Test
    @DisplayName("승인이 겹치면 하나만 성공하고 검토 알림 이벤트는 한 번만 발행된다")
    void approve_concurrently_publishesEventOnce() throws Exception {
        Long requestId = setupPendingRequest("ACTUAL");

        List<Integer> statusCodes = callConcurrently(
            () -> approve(requestId, "동시 승인 1"),
            () -> approve(requestId, "동시 승인 2")
        );

        assertSoftly(softly -> {
            softly.assertThat(statusCodes).as("응답 코드").containsExactlyInAnyOrder(200, 409);
            softly.assertThat(reviewedEventRecorder.countOf(requestId)).as("검토 알림 이벤트 수").isEqualTo(1);
        });
    }

    @Test
    @DisplayName("승인과 반려가 겹치면 하나만 성공하고 저장된 상태와 사유는 성공한 요청의 것이다")
    void approveAndReject_concurrently_keepsWinnerOnly() throws Exception {
        Long requestId = setupPendingRequest("ACTUAL");

        List<Integer> statusCodes = callConcurrently(
            () -> approve(requestId, "동시 승인"),
            () -> reject(requestId, "동시 반려")
        );

        Map<String, Object> saved = jdbcTemplate.queryForMap(
            "SELECT status, note FROM purchase_requests WHERE id = ?", requestId);
        String expectedNote = "APPROVED".equals(saved.get("status")) ? "동시 승인" : "동시 반려";
        assertSoftly(softly -> {
            softly.assertThat(statusCodes).as("응답 코드").containsExactlyInAnyOrder(200, 409);
            softly.assertThat(saved.get("note")).as("저장된 사유").isEqualTo(expectedNote);
            softly.assertThat(reviewedEventRecorder.countOf(requestId)).as("검토 알림 이벤트 수").isEqualTo(1);
        });
    }

    @Test
    @DisplayName("구매 완료 보고가 겹치면 하나만 성공하고 거래 내역은 한 벌만 남는다")
    void report_concurrently_keepsSingleTransactionSet() throws Exception {
        Long vendorId = createVendorAndCharge(100000L);
        Long requestId = setupPendingRequest("ACTUAL");
        approve(requestId, "구매 보고 준비 승인").then().statusCode(200);

        List<Integer> statusCodes = callConcurrently(
            () -> report(requestId, reportBody(vendorId, 20000L, null)),
            () -> report(requestId, reportBody(vendorId, 20000L, null))
        );

        assertSoftly(softly -> {
            softly.assertThat(statusCodes).as("응답 코드").containsExactlyInAnyOrder(200, 409);
            softly.assertThat(paymentTransactionCount(requestId)).as("거래 내역 수").isEqualTo(1);
        });
    }

    @Test
    @DisplayName("삭제와 승인이 겹치면 하나만 성공하고 진 쪽은 이긴 쪽의 결과를 보고 거절된다")
    void deleteAndApprove_concurrently_onlyOneSucceeds() throws Exception {
        Long requestId = setupPendingRequest("ACTUAL");

        List<Integer> statusCodes = callConcurrently(
            () -> delete(requestId),
            () -> approve(requestId, "동시 승인")
        );

        Map<String, Object> saved = jdbcTemplate.queryForMap(
            "SELECT status, is_deleted FROM purchase_requests WHERE id = ?", requestId);
        boolean deleted = Boolean.TRUE.equals(saved.get("is_deleted"));
        // 삭제가 이기면 승인은 삭제된 요청을 못 찾고(404), 승인이 이기면 삭제는 처리된 요청이라 거절된다(409).
        List<Integer> expectedStatusCodes = deleted ? List.of(204, 404) : List.of(409, 200);
        assertSoftly(softly -> {
            softly.assertThat(statusCodes).as("응답 코드").isEqualTo(expectedStatusCodes);
            softly.assertThat(saved.get("status")).as("저장된 상태").isEqualTo(deleted ? "PENDING" : "APPROVED");
        });
    }

    @Test
    @DisplayName("같은 영수증 첨부가 겹쳐도 품의 영수증은 한 건만 남는다")
    void attachProposalReceipt_concurrently_keepsSingleReceipt() throws Exception {
        Long requestId = setupPendingRequest("PREPAID");
        String fileId = uploadReceiptFile();

        List<Integer> statusCodes = callConcurrently(
            () -> attachProposalReceipt(requestId, fileId),
            () -> attachProposalReceipt(requestId, fileId)
        );

        assertSoftly(softly -> {
            softly.assertThat(statusCodes).as("응답 코드").containsExactly(201, 201);
            softly.assertThat(proposalCount(requestId)).as("품의 수").isEqualTo(1);
            softly.assertThat(proposalReceiptCount(requestId)).as("품의 영수증 수").isEqualTo(1);
        });
    }

    @Test
    @DisplayName("다른 트랜잭션이 구입 요청을 잠근 동안 결재 확인하면 409 BIZ005이고 잔액은 그대로다")
    void confirm_whileRowLocked_returnsResourceBusy() throws Exception {
        Long vendorId = createVendorAndCharge(100000L);
        Long requestId = setupPurchasedActualRequest(vendorId, 20000L);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<?> lockHolder = executor.submit(() -> holdRowLock(requestId, locked, release));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

            Response response = confirm(requestId);

            release.countDown();
            lockHolder.get(10, TimeUnit.SECONDS);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).as("응답 코드").isEqualTo(409);
                softly.assertThat(response.jsonPath().getString("code")).as("오류 코드").isEqualTo("BIZ005");
                softly.assertThat(vendorBalance(vendorId)).as("거래처 잔액").isEqualTo(100000L);
                softly.assertThat(balanceHistoryCount(requestId)).as("구입 요청의 잔액 이력 수").isZero();
            });
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    // ── 동시 호출 ────────────────────────────────────────

    private void holdRowLock(Long requestId, CountDownLatch locked, CountDownLatch release) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            purchaseRequestRepository.findByIdForUpdate(requestId).orElseThrow();
            locked.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @SafeVarargs
    private List<Integer> callConcurrently(Supplier<Response>... calls) throws Exception {
        CountDownLatch ready = new CountDownLatch(calls.length);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(calls.length);

        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Supplier<Response> call : calls) {
                futures.add(executor.submit(waitThenCall(call, ready, start)));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statusCodes = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statusCodes.add(future.get(30, TimeUnit.SECONDS));
            }
            return statusCodes;
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Integer> waitThenCall(Supplier<Response> call, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 호출 시작 신호를 받지 못했습니다.");
            }
            return call.get().statusCode();
        };
    }

    private Response confirm(Long requestId) {
        return given()
            .basePath(ADMIN_PATH)
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", requestId);
    }

    // ── 검증 ────────────────────────────────────────────

    private void assertProcessedOnce(List<Integer> statusCodes, Long vendorId, long expectedBalance, Long requestId) {
        assertSoftly(softly -> {
            softly.assertThat(statusCodes).as("응답 코드").containsExactlyInAnyOrder(200, 409);
            softly.assertThat(vendorBalance(vendorId)).as("거래처 잔액").isEqualTo(expectedBalance);
            softly.assertThat(balanceHistoryCount(requestId)).as("구입 요청의 잔액 이력 수").isEqualTo(1);
        });
    }

    private Long vendorBalance(Long vendorId) {
        return jdbcTemplate.queryForObject("SELECT balance FROM vendors WHERE id = ?", Long.class, vendorId);
    }

    private Integer paymentTransactionCount(Long requestId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM purchase_request_payment_transactions WHERE purchase_request_id = ?",
            Integer.class,
            requestId
        );
    }

    private Integer proposalCount(Long requestId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM purchase_request_proposals WHERE purchase_request_id = ?",
            Integer.class,
            requestId
        );
    }

    private Integer proposalReceiptCount(Long requestId) {
        return jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM purchase_request_proposal_receipts r
                JOIN purchase_request_proposals p ON p.id = r.proposal_id
                WHERE p.purchase_request_id = ?
                  AND r.is_deleted = FALSE
                """,
            Integer.class,
            requestId
        );
    }

    private Integer balanceHistoryCount(Long requestId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM vendor_balance_histories WHERE purchase_request_id = ?",
            Integer.class,
            requestId
        );
    }

    // ── 준비 ────────────────────────────────────────────

    private Long setupPendingRequest(String paymentType) {
        Long requestId = createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "동시 요청 검증", "동시 요청 검증용 구입", 20000L, paymentType);
        requestIds.add(requestId);
        return requestId;
    }

    private Long setupPurchasedActualRequest(Long vendorId, long amount) {
        Long requestId = setupPendingRequest("ACTUAL");
        approve(requestId, "구매 보고 준비 승인").then().statusCode(200);
        report(requestId, reportBody(vendorId, amount, null)).then().statusCode(200);
        return requestId;
    }

    private Long setupPurchasedPrepaidRequest(Long vendorId, long amount) {
        Long requestId = setupPendingRequest("PREPAID");
        approve(requestId, "구매 보고 준비 승인").then().statusCode(200);
        report(requestId, reportBody(vendorId, amount, "CARD")).then().statusCode(200);
        saveProposal(requestId, amount);
        attachProposalReceipt(requestId);
        return requestId;
    }

    private Response approve(Long requestId, String note) {
        return given()
            .basePath(ADMIN_PATH)
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", note))
            .patch("/{requestId}/approve", requestId);
    }

    private Response reject(Long requestId, String note) {
        return given()
            .basePath(ADMIN_PATH)
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", note))
            .patch("/{requestId}/reject", requestId);
    }

    private Response report(Long requestId, Map<String, Object> body) {
        return given()
            .basePath(USER_PATH)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(body)
            .post("/{requestId}/report", requestId);
    }

    private Map<String, Object> reportBody(Long vendorId, long amount, String paymentMethod) {
        Map<String, Object> transaction = new java.util.LinkedHashMap<>();
        transaction.put("vendorId", vendorId);
        transaction.put("itemNames", List.of("동시 요청 검증 품목"));
        transaction.put("amount", amount);
        if (paymentMethod != null) {
            transaction.put("paymentMethod", paymentMethod);
        }
        return Map.of("transactions", List.of(transaction));
    }

    private void saveProposal(Long requestId, long amount) {
        given()
            .basePath(USER_PATH)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "proposalDate", LocalDate.now().toString(),
                "completionDate", LocalDate.now().toString(),
                "proposalAmount", amount,
                "paymentAccount", "NATIONAL_SUBSIDY_04",
                "items", List.of(Map.of(
                    "content", "동시 요청 검증 품목",
                    "quantity", 1,
                    "estimatedUnitPrice", amount
                ))
            ))
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(200);
    }

    private void attachProposalReceipt(Long requestId) {
        attachProposalReceipt(requestId, uploadReceiptFile()).then().statusCode(201);
    }

    private String uploadReceiptFile() {
        String fileId = given()
            .basePath("/api/v1/files")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.MULTIPART)
            .multiPart("file", "receipt.png", "receipt".getBytes(), "image/png")
            .post("/images/purchase-items")
            .then()
            .statusCode(201)
            .extract()
            .path("fileId");
        fileIds.add(UUID.fromString(fileId));
        return fileId;
    }

    private Response attachProposalReceipt(Long requestId, String fileId) {
        return given()
            .basePath(USER_PATH)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", fileId))
            .post("/{requestId}/proposal/receipts", requestId);
    }

    private Response delete(Long requestId) {
        return given()
            .basePath(USER_PATH)
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}", requestId);
    }

    private Long createVendorAndCharge(long amount) {
        Long vendorId = given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("name", "동시 요청 테스트 거래처 " + System.nanoTime()))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
        vendorIds.add(vendorId);

        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("amount", amount, "memo", "테스트 충전"))
            .post("/{vendorId}/charges", vendorId)
            .then()
            .statusCode(200)
            .body("balance", equalTo((int) amount));

        return vendorId;
    }
}
