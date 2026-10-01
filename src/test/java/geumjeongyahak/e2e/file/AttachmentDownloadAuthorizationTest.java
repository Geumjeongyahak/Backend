package geumjeongyahak.e2e.file;

import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import io.restassured.http.ContentType;

@DisplayName("E2E: 첨부파일 다운로드 권한 — 파일이 붙은 리소스를 읽을 수 있으면 받을 수 있다")
class AttachmentDownloadAuthorizationTest extends BaseFileTest {

    private static final long NOTICE_CHANNEL_ID = 1L;
    private static final String OTHER_VOLUNTEER = "fileOtherVolunteer1234";
    private static final String VENDOR_READER = "fileVendorReader1234";

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    @Override
    public void tearDown() {
        jdbcTemplate.update("UPDATE channels SET is_active = TRUE WHERE id = ?", NOTICE_CHANNEL_ID);
        jdbcTemplate.update("DELETE FROM post_attachments");
        jdbcTemplate.update("DELETE FROM posts WHERE title = 'attachment-auth'");
        jdbcTemplate.update("DELETE FROM meeting_record_attachments");
        jdbcTemplate.update("DELETE FROM meeting_records WHERE title = 'attachment-auth'");
        jdbcTemplate.update("DELETE FROM purchase_request_proposal_receipts");
        jdbcTemplate.update("DELETE FROM purchase_request_payment_transactions");
        jdbcTemplate.update("DELETE FROM vendor_balance_histories");
        jdbcTemplate.update("""
            DELETE FROM purchase_request_proposals WHERE purchase_request_id IN
              (SELECT id FROM purchase_requests WHERE title = 'attachment-auth')""");
        jdbcTemplate.update("DELETE FROM purchase_requests WHERE title = 'attachment-auth'");
        jdbcTemplate.update("DELETE FROM site_histories WHERE title = 'attachment-auth'");
        jdbcTemplate.update("DELETE FROM vendors WHERE name = 'attachment-auth'");
        super.tearDown();
    }

    @Test
    @DisplayName("① 봉사자는 회의록 첨부를 받을 수 있다")
    void meetingRecordAttachment_volunteer_ok() {
        UUID fileId = uploadAttachment();
        attachToMeetingRecord(fileId);

        expectDownload(userAccessToken, fileId, 200);
    }

    @Test
    @DisplayName("② 봉사자는 자기 임시저장 글의 첨부를 받을 수 있다")
    void ownDraftPostAttachment_volunteer_ok() {
        UUID fileId = uploadAttachment();
        attachToPost(fileId, userTestHelper.getUser(TEST_FILE_USER).getId(), "DRAFT");

        expectDownload(userAccessToken, fileId, 200);
    }

    @Test
    @DisplayName("③ 봉사자는 남의 임시저장 글의 첨부를 받을 수 없다")
    void otherDraftPostAttachment_volunteer_forbidden() {
        userTestHelper.createTestUser(OTHER_VOLUNTEER, RoleType.VOLUNTEER);
        UUID fileId = uploadAttachment();
        attachToPost(fileId, userTestHelper.getUser(OTHER_VOLUNTEER).getId(), "DRAFT");

        expectDownload(userAccessToken, fileId, 403);
    }

    @Test
    @DisplayName("④ 봉사자는 구매 품의 영수증을 받을 수 있다")
    void purchaseProposalReceipt_volunteer_ok() {
        UUID fileId = uploadAttachment();
        attachToPurchaseProposal(fileId);

        expectDownload(userAccessToken, fileId, 200);
    }

    @Test
    @DisplayName("⑤ 게스트는 회의록 첨부를 받을 수 없다")
    void meetingRecordAttachment_guest_forbidden() {
        UUID fileId = uploadAttachment();
        attachToMeetingRecord(fileId);

        expectDownload(guestAccessToken, fileId, 403);
    }

    @Test
    @DisplayName("⑥ 게스트는 구매 품의 영수증을 받을 수 없다")
    void purchaseProposalReceipt_guest_forbidden() {
        UUID fileId = uploadAttachment();
        attachToPurchaseProposal(fileId);

        expectDownload(guestAccessToken, fileId, 403);
    }

    @Test
    @DisplayName("⑦ 어디에도 붙지 않은 파일은 관리자만 받을 수 있다")
    void unattachedFile_adminOnly() {
        UUID fileId = uploadAttachment();

        expectDownload(userAccessToken, fileId, 403);
        expectDownload(adminAccessToken, fileId, 200);
    }

    @Test
    @DisplayName("⑧ 연혁 사진 파일은 로그인한 누구나 받을 수 있다")
    void siteHistoryPhoto_anyone_ok() {
        UUID fileId = uploadAttachment();
        attachToSiteHistory(fileId);

        expectDownload(guestAccessToken, fileId, 200);
        expectDownload(userAccessToken, fileId, 200);
    }

    @Test
    @DisplayName("⑨ 봉사자는 구매 결제 영수증을 받을 수 있다")
    void paymentTransactionReceipt_volunteer_ok() {
        UUID fileId = uploadAttachment();
        attachToPaymentTransaction(fileId);

        expectDownload(userAccessToken, fileId, 200);
        expectDownload(guestAccessToken, fileId, 403);
    }

    @Test
    @DisplayName("⑩ 거래처 잔액 이력 영수증은 거래처 이력을 볼 수 있는 사람(vendor:read:*)만 받는다")
    void vendorBalanceReceipt_onlyVendorReaders() {
        UUID fileId = uploadAttachment();
        attachToVendorBalanceHistory(fileId);
        User vendorReader = userTestHelper.createTestUser(VENDOR_READER, RoleType.GUEST);
        userPermissionRepository.save(new UserPermission(vendorReader, "vendor:read:*"));

        expectDownload(userAccessToken, fileId, 403);
        expectDownload(userTestHelper.generateAccessTokenByUserKey(VENDOR_READER), fileId, 200);
        expectDownload(adminAccessToken, fileId, 200);
    }

    @Test
    @DisplayName("⑪ 채널을 읽을 수 없게 되면 자기 임시저장 글의 첨부도 받을 수 없다 (글 조회와 같은 규칙)")
    void ownDraftAttachment_channelNotReadable_forbidden() {
        UUID fileId = uploadAttachment();
        attachToPost(fileId, userTestHelper.getUser(TEST_FILE_USER).getId(), "DRAFT");
        jdbcTemplate.update("UPDATE channels SET is_active = FALSE WHERE id = ?", NOTICE_CHANNEL_ID);

        expectDownload(userAccessToken, fileId, 403);
    }

    @Test
    @DisplayName("⑫ 지운 구매 요청의 품의·결제 영수증은 받을 수 없다")
    void deletedPurchaseRequestReceipts_forbidden() {
        UUID proposalReceipt = uploadAttachment();
        attachToPurchaseProposal(proposalReceipt);
        UUID paymentReceipt = uploadAttachment();
        attachToPaymentTransaction(paymentReceipt);
        jdbcTemplate.update("UPDATE purchase_requests SET is_deleted = TRUE WHERE title = 'attachment-auth'");

        expectDownload(userAccessToken, proposalReceipt, 403);
        expectDownload(userAccessToken, paymentReceipt, 403);
    }

    private UUID uploadAttachment() {
        return UUID.fromString(
            given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.MULTIPART)
                .multiPart("file", "report.pdf", "fake-pdf".getBytes(StandardCharsets.UTF_8), "application/pdf")
            .when()
                .post("/attachments")
            .then()
                .statusCode(201)
                .extract()
                .path("fileId")
        );
    }

    private void expectDownload(String accessToken, UUID fileId, int status) {
        given()
            .header(AUTH_HEADER, getAuthHeader(accessToken))
        .when()
            .get("/attachments/{fileId}/download-url", fileId)
        .then()
            .statusCode(status);
    }

    private Long adminId() {
        return userTestHelper.getUser(TEST_ADMIN_USERNAME).getId();
    }

    private void attachToPost(UUID fileId, Long authorId, String status) {
        jdbcTemplate.update(
            "INSERT INTO posts (channel_id, author_id, title, content_html, status) VALUES (?, ?, 'attachment-auth', '<p>x</p>', ?)",
            NOTICE_CHANNEL_ID, authorId, status);
        Long postId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM posts", Long.class);
        jdbcTemplate.update("INSERT INTO post_attachments (post_id, file_id) VALUES (?, ?)", postId, fileId);
    }

    private void attachToMeetingRecord(UUID fileId) {
        jdbcTemplate.update(
            "INSERT INTO meeting_records (author_id, title, agenda) VALUES (?, 'attachment-auth', '안건')", adminId());
        Long recordId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM meeting_records", Long.class);
        jdbcTemplate.update(
            "INSERT INTO meeting_record_attachments (meeting_record_id, file_id) VALUES (?, ?)", recordId, fileId);
    }

    private void attachToPurchaseProposal(UUID fileId) {
        jdbcTemplate.update("""
            INSERT INTO purchase_requests (requested_by, payment_type, title, total_price, status)
            VALUES (?, 'PREPAID', 'attachment-auth', 1000, 'PENDING')""", adminId());
        Long requestId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM purchase_requests", Long.class);
        jdbcTemplate.update(
            "INSERT INTO purchase_request_proposals (purchase_request_id, proposal_date) VALUES (?, ?)",
            requestId, LocalDate.of(2026, 9, 1));
        Long proposalId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM purchase_request_proposals", Long.class);
        jdbcTemplate.update(
            "INSERT INTO purchase_request_proposal_receipts (proposal_id, file_id, sort_order) VALUES (?, ?, 0)",
            proposalId, fileId);
    }

    private Long createVendor() {
        jdbcTemplate.update("INSERT INTO vendors (name) VALUES ('attachment-auth')");
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM vendors", Long.class);
    }

    private void attachToPaymentTransaction(UUID fileId) {
        jdbcTemplate.update("""
            INSERT INTO purchase_requests (requested_by, payment_type, title, total_price, status)
            VALUES (?, 'PREPAID', 'attachment-auth', 1000, 'PENDING')""", adminId());
        Long requestId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM purchase_requests", Long.class);
        jdbcTemplate.update(
            "INSERT INTO purchase_request_payment_transactions (purchase_request_id, vendor_id, amount, receipt_file_id) VALUES (?, ?, 1000, ?)",
            requestId, createVendor(), fileId);
    }

    private void attachToVendorBalanceHistory(UUID fileId) {
        jdbcTemplate.update("""
            INSERT INTO vendor_balance_histories (vendor_id, type, amount, balance_after, receipt_file_id, created_by, occurred_at)
            VALUES (?, 'CHARGE', 1000, 1000, ?, ?, CURRENT_TIMESTAMP)""", createVendor(), fileId, adminId());
    }

    private void attachToSiteHistory(UUID fileId) {
        jdbcTemplate.update(
            "INSERT INTO site_histories (title, history_date) VALUES ('attachment-auth', ?)", LocalDate.of(2001, 3, 1));
        Long historyId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM site_histories", Long.class);
        jdbcTemplate.update(
            "INSERT INTO site_history_photos (history_id, file_id, src) VALUES (?, ?, 'https://example.com/a.jpg')",
            historyId, fileId);
    }
}
