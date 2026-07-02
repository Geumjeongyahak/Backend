package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StudentAttendanceSheetRequest(
    @NotNull
    @Min(2000)
    @Max(2100)
    @Schema(description = "조회 연도", example = "2026")
    Integer year,

    @NotNull
    @Min(1)
    @Max(12)
    @Schema(description = "조회 월", example = "2")
    Integer month,

    @NotNull
    @Positive
    @Schema(description = "분반 ID", example = "1")
    Long classroomId
) {
}
