package sonmoeum.domain.request.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import sonmoeum.domain.request.entity.SubjectExchangeRequest;
import sonmoeum.domain.request.enums.RequestStatus;

public record SubjectExchangeRequestResponse(

    @Schema(description = "요청 ID", example = "1")
    Long id,

    @Schema(description = "과목 ID", example = "1")
    Long subjectId,

    @Schema(description = "과목 이름", example = "한글 기초")
    String subjectName,

    @Schema(description = "요청자 ID", example = "3")
    Long requestedById,

    @Schema(description = "요청자 이름", example = "홍길동")
    String requestedByName,

    @Schema(description = "요청 제목")
    String title,

    @Schema(description = "요청 내용")
    String content,

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
    public static SubjectExchangeRequestResponse from(SubjectExchangeRequest r) {
        return new SubjectExchangeRequestResponse(
            r.getId(),
            r.getSubject().getId(),
            r.getSubject().getName(),
            r.getRequestedBy().getId(),
            r.getRequestedBy().getName(),
            r.getTitle(),
            r.getContent(),
            r.getStatus(),
            r.getApprovalAt(),
            r.getApprovalBy() != null ? r.getApprovalBy().getName() : null,
            r.getNote(),
            r.getCreatedAt()
        );
    }
}
