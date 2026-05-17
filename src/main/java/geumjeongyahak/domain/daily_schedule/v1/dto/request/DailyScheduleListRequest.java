package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidDailyScheduleRange;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@ValidDailyScheduleRange
public record DailyScheduleListRequest(
    @NotNull
    @DateTimeFormat(iso = DATE)
    @Schema(description = "시작 일자", example = "2026-06-01")
    LocalDate from,

    @NotNull
    @DateTimeFormat(iso = DATE)
    @Schema(description = "종료 일자", example = "2026-06-30")
    LocalDate to,

    @Schema(description = "분반 ID", example = "1")
    Long classroomId,

    @Schema(description = "담당 교사 ID", example = "2")
    Long teacherId,

    @Schema(
        description = "하루 일정 상태",
        example = "SCHEDULED",
        allowableValues = {"SCHEDULED", "COMPLETED", "CANCELLED"}
    )
    DailyScheduleStatus status
) {
}
