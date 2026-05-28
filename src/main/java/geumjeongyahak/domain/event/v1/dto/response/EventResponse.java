package geumjeongyahak.domain.event.v1.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import geumjeongyahak.domain.event.entity.Event;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "행사 응답")
public record EventResponse(
    @Schema(description = "행사 ID", example = "1")
    Long id,

    @Schema(description = "행사 제목", example = "문학의 밤")
    String title,

    @Schema(description = "행사 설명", example = "문학의 밤 행사에 대한 상세 설명입니다.")
    String description,

    @Schema(description = "행사 날짜", example = "2026-05-13")
    LocalDate eventDate,

    @Schema(description = "행사 시작 시간", example = "19:00:00", nullable = true)
    LocalTime startTime,

    @Schema(description = "행사 종료 시간", example = "21:00:00", nullable = true)
    LocalTime endTime,

    @Schema(description = "마지막 수정자 User ID", example = "1")
    Long lastModifiedById,

    @Schema(description = "마지막 수정자 이름", example = "관리자")
    String lastModifiedByName,

    @Schema(description = "생성 일시", example = "2026-05-01T10:00:00")
    LocalDateTime createdAt,

    @Schema(description = "수정 일시", example = "2026-05-01T10:00:00")
    LocalDateTime updatedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
            event.getId(),
            event.getTitle(),
            event.getDescription(),
            event.getEventDate(),
            event.getStartTime(),
            event.getEndTime(),
            event.getUpdatedBy().getId(),
            event.getUpdatedBy().getName(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
}
