package geumjeongyahak.domain.request.v1.dto.response;

import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record LessonExchangeRequestSummaryResponse(

    @Schema(description = "요청 ID", example = "1")
    Long id,

    @Schema(description = "반 이름", example = "개나리반")
    String classroomName,

    @Schema(description = "요청자 이름", example = "홍길동")
    String requestedByName,

    @Schema(description = "요청 제목")
    String title,

    @Schema(description = "요청 상태", example = "PENDING")
    LessonExchangeRequestStatus status,

    @Schema(description = "생성 시각")
    LocalDateTime createdAt
) {
    public static LessonExchangeRequestSummaryResponse from(LessonExchangeRequest r) {
        return new LessonExchangeRequestSummaryResponse(
            r.getId(),
            r.getClassroomNameSnapshot(),
            r.getRequestedBy().getName(),
            r.getTitle(),
            r.getStatus(),
            r.getCreatedAt()
        );
    }
}

