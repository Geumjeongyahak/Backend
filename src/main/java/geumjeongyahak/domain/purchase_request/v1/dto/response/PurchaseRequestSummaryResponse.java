package geumjeongyahak.domain.purchase_request.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;

public record PurchaseRequestSummaryResponse(

    @Schema(description = "요청 ID", example = "1")
    Long id,

    @Schema(description = "요청 대상 분반 ID. 부서 대상 요청이면 null입니다.", example = "1", nullable = true)
    Long classroomId,

    @Schema(description = "요청 대상 분반 이름. 부서 대상 요청이면 null입니다.", example = "한글반", nullable = true)
    String classroomName,

    @Schema(description = "요청 대상 부서 ID. 분반 대상 요청이면 null입니다.", example = "2", nullable = true)
    Long departmentId,

    @Schema(description = "요청 대상 부서 이름. 분반 대상 요청이면 null입니다.", example = "교육연구부", nullable = true)
    String departmentName,

    @Schema(description = "요청자 ID", example = "3")
    Long requestedById,

    @Schema(description = "요청자 이름", example = "홍길동")
    String requestedByName,

    @Schema(description = "구입 요청 제목", example = "교재 구입")
    String title,

    @Schema(description = "결제 유형", example = "ACTUAL")
    PurchasePaymentType paymentType,

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
            r.getClassroom() != null ? r.getClassroom().getId() : null,
            r.getClassroom() != null ? r.getClassroom().getName() : null,
            r.getDepartment() != null ? r.getDepartment().getId() : null,
            r.getDepartment() != null ? r.getDepartment().getName() : null,
            r.getRequestedBy().getId(),
            r.getRequestedBy().getName(),
            r.getTitle(),
            r.getPaymentType(),
            r.getTotalPrice(),
            r.getStatus(),
            r.getCreatedAt()
        );
    }
}
