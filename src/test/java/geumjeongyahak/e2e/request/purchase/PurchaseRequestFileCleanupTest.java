package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import geumjeongyahak.domain.file.config.FileCleanupProperties;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.file.service.FileCleanupScheduler;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("purchase-request")
@DisplayName("E2E: 구매 요청 영수증 파일 청소 테스트")
class PurchaseRequestFileCleanupTest extends RequestBaseTest {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private FileCleanupScheduler fileCleanupScheduler;

    @Autowired
    private FileCleanupProperties fileCleanupProperties;

    private UUID uploadedFileId;
    private Long createdRequestId;
    private Long originalTemporaryRetentionHours;

    @AfterEach
    void cleanUp() {
        if (originalTemporaryRetentionHours != null) {
            ReflectionTestUtils.setField(fileCleanupProperties, "temporaryRetentionHours", originalTemporaryRetentionHours);
        }
        if (createdRequestId != null && purchaseRequestRepository.existsById(createdRequestId)) {
            purchaseRequestRepository.deleteById(createdRequestId);
        }
        if (uploadedFileId != null && fileRepository.existsById(uploadedFileId)) {
            fileRepository.deleteById(uploadedFileId);
        }
    }

    @Test
    @DisplayName("오래된 미연동 구매 영수증 파일은 스케줄러가 soft delete 한다")
    void cleanupScheduler_softDeletesUnlinkedPurchaseReceiptUpload() {
        useImmediateTemporaryRetention();
        uploadedFileId = UUID.fromString(uploadPurchaseReceipt());

        fileCleanupScheduler.cleanupDeletedFiles();

        File file = fileRepository.findById(uploadedFileId).orElseThrow();
        assertThat(file.isDeleted()).isTrue();
        assertThat(file.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("구매 요청 품목에 연결된 영수증 파일은 임시 파일 청소 대상에서 제외된다")
    void cleanupScheduler_keepsLinkedPurchaseReceiptUpload() {
        useImmediateTemporaryRetention();
        uploadedFileId = UUID.fromString(uploadPurchaseReceipt());

        createdRequestId = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "영수증 연결 구입 요청"),
                entry("content", "영수증 파일이 품목에 연결된 요청입니다."),
                entry("classroomId", CLASSROOM_ID),
                entry("paymentMethod", "NORMAL"),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "복사용지"),
                    entry("reason", "수업 자료 출력"),
                    entry("price", 10000L),
                    entry("receiptFileId", uploadedFileId.toString())
                )))
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        fileCleanupScheduler.cleanupDeletedFiles();

        File file = fileRepository.findById(uploadedFileId).orElseThrow();
        assertThat(file.isDeleted()).isFalse();
        assertThat(file.getDeletedAt()).isNull();
    }

    private void useImmediateTemporaryRetention() {
        if (originalTemporaryRetentionHours == null) {
            originalTemporaryRetentionHours = fileCleanupProperties.getTemporaryRetentionHours();
        }
        ReflectionTestUtils.setField(fileCleanupProperties, "temporaryRetentionHours", 0L);
    }

    private String uploadPurchaseReceipt() {
        return given()
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .multiPart("file", "receipt.png", "receipt".getBytes(), "image/png")
            .post("/api/v1/files/images/purchase-items")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("fileId");
    }
}
