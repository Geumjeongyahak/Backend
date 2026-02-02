package sonmoeum.api.v1.requests.dto.response;

import java.time.LocalDateTime;

import sonmoeum.domain.request.entity.AbsenceRequest;
import sonmoeum.domain.request.enums.RequestStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record AbsenceRequestResponse(
    @Schema(description = "요청 ID", example = "1")
    Long id,
    @Schema(description = "수업 ID", example = "1")
    Long lessonId,
    @Schema(description = "요청자 ID", example = "1")
    Long requestedBy,
    @Schema(description = "결석 사유", example = "개인 사정")
    String reason,
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
    public static AbsenceRequestResponse from(AbsenceRequest request) {
        return new AbsenceRequestResponse(
            request.getId(),
            request.getLesson().getId(),
            request.getRequestedBy().getId(),
            request.getReason(),
            request.getStatus(),
            request.getApprovalAt(),
            request.getApprovalBy() != null ? request.getApprovalBy().getId() : null,
            request.getNote(),
            request.getCreatedAt()
        );
    }
}
