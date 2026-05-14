package geumjeongyahak.domain.request.v1.dto.response;

import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LessonExchangeRequestDetailResponse(

    @Schema(description = "요청 ID", example = "1")
    Long id,

    @Schema(description = "반 이름", example = "벚꽃반")
    String classroomName,

    @Schema(description = "수업 날짜", example = "2026-06-10")
    LocalDate lessonDate,

    @Schema(description = "요청자 ID", example = "2")
    Long requestedById,

    @Schema(description = "요청자 이름", example = "홍길동")
    String requestedByName,

    @Schema(description = "요청 제목")
    String title,

    @Schema(description = "요청 내용")
    String content,

    @Schema(description = "요청 상태", example = "PENDING")
    LessonExchangeRequestStatus status,

    @Schema(description = "요청 만료 시각", example = "2026-06-07T22:00:00")
    LocalDateTime expiresAt,

    @Schema(description = "처리(승인/반려) 시각")
    LocalDateTime processedAt,

    @Schema(description = "처리자 이름")
    String processedByName,

    @Schema(description = "반려 메모")
    String rejectionNote,

    @Schema(description = "교환 완료 시각")
    LocalDateTime completedAt,

    @Schema(description = "요청 취소 시각")
    LocalDateTime cancelledAt,

    @Schema(description = "생성 시각")
    LocalDateTime createdAt
) {
    public static LessonExchangeRequestDetailResponse from(LessonExchangeRequest r) {
        return new LessonExchangeRequestDetailResponse(
            r.getId(),
            r.getClassroomNameSnapshot(),
            r.getLessonDate(),
            r.getRequestedBy().getId(),
            r.getRequestedBy().getName(),
            r.getTitle(),
            r.getContent(),
            r.getStatus(),
            r.getExpiresAt(),
            r.getProcessedAt(),
            r.getProcessedBy() != null ? r.getProcessedBy().getName() : null,
            r.getRejectionNote(),
            r.getCompletedAt(),
            r.getCancelledAt(),
            r.getCreatedAt()
        );
    }
}
