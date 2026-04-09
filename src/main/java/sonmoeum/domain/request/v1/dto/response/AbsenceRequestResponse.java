package sonmoeum.domain.request.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import sonmoeum.domain.request.entity.AbsenceRequest;
import sonmoeum.domain.request.enums.RequestStatus;

public record AbsenceRequestResponse(

    @Schema(description = "요청 ID", example = "1")
    Long id,

    @Schema(description = "수업 ID", example = "1")
    Long lessonId,

    @Schema(description = "수업 날짜", example = "2026-03-28")
    LocalDate lessonDate,

    @Schema(description = "요청자 ID", example = "3")
    Long requestedById,

    @Schema(description = "요청자 이름", example = "홍길동")
    String requestedByName,

    @Schema(description = "결석 사유")
    String reason,

    @Schema(description = "요청 상태", example = "PENDING")
    RequestStatus status,

    @Schema(description = "승인/반려 시각")
    LocalDateTime approvalAt,

    @Schema(description = "처리자 이름")
    String approvalByName,

    @Schema(description = "처리 메모")
    String note,

    @Schema(description = "생성 시각")
    LocalDateTime createdAt
) {
    public static AbsenceRequestResponse from(AbsenceRequest r) {
        return new AbsenceRequestResponse(
            r.getId(),
            r.getLesson().getId(),
            r.getLesson().getDate(),
            r.getRequestedBy().getId(),
            r.getRequestedBy().getName(),
            r.getReason(),
            r.getStatus(),
            r.getApprovalAt(),
            r.getApprovalBy() != null ? r.getApprovalBy().getName() : null,
            r.getNote(),
            r.getCreatedAt()
        );
    }
}
