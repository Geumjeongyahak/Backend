package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidDailyTeacherAttendanceCorrection;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@ValidDailyTeacherAttendanceCorrection
public record UpdateDailyTeacherAttendanceCorrectionRequest(
    @NotNull
    @Schema(
        description = "보정할 교사 출석 상태",
        example = "PRESENT",
        allowableValues = {"PRESENT", "ABSENT", "LATE", "EXCUSED"}
    )
    DailyTeacherAttendanceStatus status,

    @Schema(description = "보정할 교사 출근 시각. status가 ABSENT이면 입력할 수 없습니다.", example = "2026-06-20T14:00:00")
    LocalDateTime attendedAt,

    @Schema(description = "보정할 교사 퇴근 시각. 퇴근 처리 전 상태로 보정하려면 null을 입력합니다.", example = "2026-06-20T16:00:00")
    LocalDateTime checkedOutAt
) {}
