package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateDailyScheduleStatusRequest(
    @NotNull
    @Schema(
        description = "하루 일정 상태",
        example = "COMPLETED",
        allowableValues = {"SCHEDULED", "COMPLETED", "CANCELLED"}
    )
    DailyScheduleStatus status
) {}
