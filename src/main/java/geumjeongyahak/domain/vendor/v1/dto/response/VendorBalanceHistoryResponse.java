package geumjeongyahak.domain.vendor.v1.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import geumjeongyahak.domain.vendor.entity.VendorBalanceHistory;
import geumjeongyahak.domain.vendor.enums.VendorBalanceHistoryType;
import io.swagger.v3.oas.annotations.media.Schema;

public record VendorBalanceHistoryResponse(
    @Schema(description = "이력 ID", example = "1")
    Long id,

    @Schema(description = "이력 유형", example = "CHARGE")
    VendorBalanceHistoryType type,

    @Schema(description = "금액", example = "100000")
    Long amount,

    @Schema(description = "처리 후 잔액", example = "85000")
    Long balanceAfter,

    @Schema(description = "메모")
    String memo,

    @Schema(description = "영수증 파일 ID")
    UUID receiptFileId,

    @Schema(description = "영수증 URL")
    String receiptFileUrl,

    @Schema(description = "연결된 결제 요청 ID")
    Long purchaseRequestId,

    @Schema(description = "처리자 이름")
    String createdByName,

    @Schema(description = "발생 시각")
    LocalDateTime occurredAt
) {
    public static VendorBalanceHistoryResponse from(VendorBalanceHistory history) {
        return new VendorBalanceHistoryResponse(
            history.getId(),
            history.getType(),
            history.getAmount(),
            history.getBalanceAfter(),
            history.getMemo(),
            history.getReceiptFile() != null ? history.getReceiptFile().getId() : null,
            history.getReceiptFile() != null ? history.getReceiptFile().getPublicUrl() : null,
            history.getPurchaseRequest() != null ? history.getPurchaseRequest().getId() : null,
            history.getCreatedBy().getName(),
            history.getOccurredAt()
        );
    }
}
