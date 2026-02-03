package sonmoeum.api.v1.requests.dto.response;

import java.time.LocalDateTime;

import sonmoeum.domain.request.entity.LessonExchangeRequest;
import sonmoeum.domain.request.entity.SubjectExchangeRequest;
import sonmoeum.domain.request.enums.RequestStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record ExchangeRequestResponse(
    @Schema(description = "요청 ID", example = "1")
    Long id,
    @Schema(description = "대상 ID (lessonId/subjectId)", example = "1")
    Long targetId,
    @Schema(description = "요청 타입", example = "LESSON | SUBJECT")
    String type,
    @Schema(description = "요청자 ID", example = "1")
    Long requestedBy,
    @Schema(description = "제목", example = "교환 요청")
    String title,
    @Schema(description = "내용", example = "교환 사유")
    String content,
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
    public static ExchangeRequestResponse from(LessonExchangeRequest request) {
        return new ExchangeRequestResponse(
            request.getId(),
            request.getLesson().getId(),
            "LESSON",
            request.getRequestedBy().getId(),
            request.getTitle(),
            request.getContent(),
            request.getStatus(),
            request.getApprovalAt(),
            request.getApprovalBy() != null ? request.getApprovalBy().getId() : null,
            request.getNote(),
            request.getCreatedAt()
        );
    }


    public static ExchangeRequestResponse from(SubjectExchangeRequest request) {
        return new ExchangeRequestResponse(
            request.getId(),
            request.getSubject().getId(),
            "SUBJECT",
            request.getRequestedBy().getId(),
            request.getTitle(),
            request.getContent(),
            request.getStatus(),
            request.getApprovalAt(),
            request.getApprovalBy() != null ? request.getApprovalBy().getId() : null,
            request.getNote(),
            request.getCreatedAt()
        );
    }

}
