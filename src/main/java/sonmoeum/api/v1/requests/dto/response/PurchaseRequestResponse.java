package sonmoeum.api.v1.requests.dto.response;

import java.time.LocalDateTime;

import sonmoeum.domain.request.entity.PurchaseRequest;
import sonmoeum.domain.request.enums.RequestStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record PurchaseRequestResponse(
    @Schema(description = "요청 ID", example = "1")
    Long id,
    @Schema(description = "과목 ID", example = "1")
    Long subjectId,
    @Schema(description = "요청자 ID", example = "1")
    Long requestedBy,
    @Schema(description = "제목", example = "교구 구입")
    String title,
    @Schema(description = "내용", example = "필요 교구")
    String content,
    @Schema(description = "가격", example = "50000")
    Long price,
    @Schema(description = "상태", example = "PENDING")
    RequestStatus status,
    @Schema(description = "승인/반려 일시")
    LocalDateTime approvalAt,
    @Schema(description = "승인/반려자 ID")
    Long approvalBy,
    @Schema(description = "비고")
    String note,
    @Schema(description = "생성 일시")
    LocalDateTime createdAt
) {
    public static PurchaseRequestResponse from(PurchaseRequest request) {
        return new PurchaseRequestResponse(
            request.getId(),
            request.getSubject().getId(),
            request.getRequestedBy().getId(),
            request.getTitle(),
            request.getContent(),
            request.getPrice(),
            request.getStatus(),
            request.getApprovalAt(),
            request.getApprovalBy() != null ? request.getApprovalBy().getId() : null,
            request.getNote(),
            request.getCreatedAt()
        );
    }
}
