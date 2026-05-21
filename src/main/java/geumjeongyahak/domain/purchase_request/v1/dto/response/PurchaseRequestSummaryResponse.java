package geumjeongyahak.domain.purchase_request.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;

public record PurchaseRequestSummaryResponse(

    @Schema(description = "요청 ID", example = "1")
    Long id,

    @Schema(description = "분반 이름", example = "한글반")
    String classroomName,

    @Schema(description = "요청자 이름", example = "홍길동")
    String requestedByName,

    @Schema(description = "구입 요청 제목", example = "교재 구입")
    String title,

    @Schema(description = "총 구매 금액 (원) - 구매 보고 이후 확정", example = "45000")
    Long totalPrice,

    @Schema(description = "요청 상태", example = "PENDING")
    PurchaseRequestStatus status,

    @Schema(description = "생성 시각")
    LocalDateTime createdAt
) {
    public static PurchaseRequestSummaryResponse from(PurchaseRequest r) {
        return new PurchaseRequestSummaryResponse(
            r.getId(),
            r.getClassroom().getName(),
            r.getRequestedBy().getName(),
            r.getTitle(),
            r.getTotalPrice(),
            r.getStatus(),
            r.getCreatedAt()
        );
    }
}
