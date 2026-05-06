package geumjeongyahak.domain.purchase_request.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestItem;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;

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

    @Schema(description = "생성 시각")
    LocalDateTime createdAt
) {
    public record ItemResponse(
        Long id,
        String name,
        String reason,
        Long price,
        String receiptFileUrl
    ) {
        static ItemResponse from(PurchaseRequestItem item) {
            return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getReason(),
                item.getPrice(),
                item.getReceiptFile() != null ? item.getReceiptFile().getPublicUrl() : null
            );
        }
    }

    public static PurchaseRequestDetailResponse from(PurchaseRequest r) {
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
            r.getCreatedAt()
        );
    }
}
