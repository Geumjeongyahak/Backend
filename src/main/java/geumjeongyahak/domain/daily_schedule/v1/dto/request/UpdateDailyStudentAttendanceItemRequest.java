package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import geumjeongyahak.domain.daily_schedule.enums.DailyStudentAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateDailyStudentAttendanceItemRequest(
    @NotNull
    @Schema(description = "학생 ID", example = "1")
    Long studentId,

    @NotNull
    @Schema(description = "학생 출석 상태", example = "PRESENT")
    DailyStudentAttendanceStatus status
) {}
