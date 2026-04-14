package geumjeongyahak.domain.lesson.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateLessonRequest(
    @Schema(description = "과목 ID", example = "1")
    Long subjectId,

    @Schema(description = "강사 ID", example = "2")
    Long teacherId,

    @Schema(description = "수업 일자", example = "2026-02-20")
    LocalDate date,

    @Schema(description = "시작 시간", example = "19:20:00")
    LocalTime startTime,

    @Schema(description = "종료 시간", example = "20:00:00")
    LocalTime endTime,

    @Schema(description = "교시", example = "1")
    Integer period
) {}
