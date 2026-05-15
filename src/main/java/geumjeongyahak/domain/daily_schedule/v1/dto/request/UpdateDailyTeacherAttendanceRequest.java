package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidDailyTeacherAttendanceLocation;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@ValidDailyTeacherAttendanceLocation
public record UpdateDailyTeacherAttendanceRequest(
    @NotNull
    @Schema(
        description = "교사 출석 상태",
        example = "PRESENT",
        allowableValues = {"PRESENT", "ABSENT", "LATE", "EXCUSED"}
    )
    DailyTeacherAttendanceStatus status,

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Schema(description = "출석 처리 위치 위도", example = "35.1795543")
    BigDecimal latitude,

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Schema(description = "출석 처리 위치 경도", example = "129.0756416")
    BigDecimal longitude
) {}
