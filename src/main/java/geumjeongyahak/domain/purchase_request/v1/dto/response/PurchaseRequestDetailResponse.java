package geumjeongyahak.domain.purchase_request.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestItem;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestPaymentTransaction;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.vendor.v1.dto.response.VendorResponse;

public record PurchaseRequestDetailResponse(

    @Schema(description = "요청 ID", example = "1")
    Long id,

    @Schema(description = "분반 ID", example = "1")
    Long classroomId,

    @Schema(description = "분반 이름", example = "한글반")
    String classroomName,

    @Schema(description = "요청자 ID", example = "3")
    Long requestedById,

    @Schema(description = "요청자 이름", example = "홍길동")
    String requestedByName,

    @Schema(description = "구입 요청 제목")
    String title,

    @Schema(description = "구입 요청 내용")
    String content,

    @Schema(description = "총 구매 금액 (원) - 구매 보고 이후 확정", example = "45000")
    Long totalPrice,

    @Schema(description = "요청 상태", example = "PENDING")
    PurchaseRequestStatus status,

    @Schema(description = "승인/반려 시각")
    LocalDateTime approvalAt,

    @Schema(description = "처리자 이름")
    String approvalByName,

    @Schema(description = "구매 완료 보고 시각")
    LocalDateTime purchasedAt,

    @Schema(description = "처리 메모")
    String note,

    @Schema(description = "구입 항목 목록")
    List<ItemResponse> items,

    @Schema(description = "구매 완료 거래 목록")
    List<TransactionResponse> transactions,

    @Schema(description = "현재 거래처별 잔액")
    List<VendorBalanceResponse> vendorBalances,

    @Schema(description = "생성 시각")
    LocalDateTime createdAt
) {
    public record ItemResponse(
        Long id,
        String name,
        String reason,
        Integer quantity,
        PurchasePaymentType paymentType
    ) {
        static ItemResponse from(PurchaseRequestItem item) {
            return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getReason(),
                item.getQuantity(),
                item.getPaymentType()
            );
        }
    }

    public record TransactionResponse(
        Long id,
        Long vendorId,
        String vendorName,
        List<String> itemNames,
        Long amount,
        java.util.UUID receiptFileId,
        String receiptFileUrl
    ) {
        static TransactionResponse from(PurchaseRequestPaymentTransaction transaction) {
            return new TransactionResponse(
                transaction.getId(),
                transaction.getVendor().getId(),
                transaction.getVendor().getName(),
                transaction.getItemNames(),
                transaction.getAmount(),
                transaction.getReceiptFile() != null ? transaction.getReceiptFile().getId() : null,
                transaction.getReceiptFile() != null ? transaction.getReceiptFile().getPublicUrl() : null
            );
        }
    }

    public record VendorBalanceResponse(
        Long vendorId,
        String vendorName,
        Long balance
    ) {
        public static VendorBalanceResponse from(VendorResponse vendor) {
            return new VendorBalanceResponse(
                vendor.id(),
                vendor.name(),
                vendor.balance()
            );
        }
    }

    public static PurchaseRequestDetailResponse from(PurchaseRequest r, List<VendorResponse> vendors) {
        return new PurchaseRequestDetailResponse(
            r.getId(),
            r.getClassroom().getId(),
            r.getClassroom().getName(),
            r.getRequestedBy().getId(),
            r.getRequestedBy().getName(),
            r.getTitle(),
            r.getContent(),
            r.getTotalPrice(),
            r.getStatus(),
            r.getApprovalAt(),
            r.getApprovalBy() != null ? r.getApprovalBy().getName() : null,
            r.getPurchasedAt(),
            r.getNote(),
            r.getItems().stream().map(ItemResponse::from).toList(),
            r.getTransactions().stream().map(TransactionResponse::from).toList(),
            vendors.stream().map(VendorBalanceResponse::from).toList(),
            r.getCreatedAt()
        );
    }
}
