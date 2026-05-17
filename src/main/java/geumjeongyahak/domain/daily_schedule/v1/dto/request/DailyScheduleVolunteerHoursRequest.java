package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

import geumjeongyahak.common.validation.annotation.ValidDailyScheduleVolunteerHoursRange;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@ValidDailyScheduleVolunteerHoursRange
public record DailyScheduleVolunteerHoursRequest(
    @DateTimeFormat(iso = DATE)
    @Schema(description = "시작 일자. 입력하지 않으면 시작 제한 없이 조회합니다.", example = "2026-06-01")
    LocalDate from,

    @DateTimeFormat(iso = DATE)
    @Schema(description = "종료 일자. 입력하지 않으면 종료 제한 없이 조회합니다.", example = "2026-06-30")
    LocalDate to,

    @Schema(description = "교사 ID. 입력하지 않으면 본인 기준으로 조회합니다.", example = "2")
    Long teacherId
) {
}
