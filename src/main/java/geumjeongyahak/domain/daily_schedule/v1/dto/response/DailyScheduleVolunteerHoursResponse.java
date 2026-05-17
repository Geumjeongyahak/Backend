package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record DailyScheduleVolunteerHoursResponse(
    @Schema(description = "교사 ID", example = "2")
    Long teacherId,

    @Schema(description = "조회 시작 일자. 시작 제한 없이 조회한 경우 null입니다.", example = "2026-06-01")
    LocalDate from,

    @Schema(description = "조회 종료 일자. 종료 제한 없이 조회한 경우 null입니다.", example = "2026-06-30")
    LocalDate to,

    @Schema(description = "총 봉사 인정 시간(분)", example = "360")
    Long totalVolunteerServiceMinutes,

    @Schema(description = "총 봉사 인정 시간(시간)", example = "6.00")
    BigDecimal totalVolunteerServiceHours
) {

    public static DailyScheduleVolunteerHoursResponse of(
        Long teacherId,
        LocalDate from,
        LocalDate to,
        Long totalVolunteerServiceMinutes
    ) {
        BigDecimal hours = BigDecimal.valueOf(totalVolunteerServiceMinutes)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return new DailyScheduleVolunteerHoursResponse(
            teacherId,
            from,
            to,
            totalVolunteerServiceMinutes,
            hours
        );
    }
}
