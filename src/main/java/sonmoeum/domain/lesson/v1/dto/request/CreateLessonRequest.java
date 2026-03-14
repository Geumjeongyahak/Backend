package sonmoeum.domain.lesson.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import sonmoeum.common.validation.annotation.ValidLessonTime;

@ValidLessonTime
@Schema(description = "수업 생성 요청")
public record CreateLessonRequest(
    @NotNull
    @Schema(description = "과목 ID", example = "1")
    Long subjectId,

    @NotNull
    @Schema(description = "교사(봉사자) ID", example = "2")
    Long teacherId,

    @NotNull
    @Schema(description = "수업 일자", example = "2026-02-20")
    LocalDate date,

    @NotNull
    @Schema(description = "수업 시작 시간", example = "19:20:00")
    LocalTime startTime,

    @NotNull
    @Schema(description = "수업 종료 시간", example = "20:00:00")
    LocalTime endTime,

    @NotNull
    @Schema(description = "수업 교시", example = "1")
    Integer period
) {}
