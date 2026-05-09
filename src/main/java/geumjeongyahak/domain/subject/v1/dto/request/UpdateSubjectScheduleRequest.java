package geumjeongyahak.domain.subject.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidSubjectSchedule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@ValidSubjectSchedule
@Schema(description = "과목 일정 수정 요청")
public record UpdateSubjectScheduleRequest(
    @Schema(description = "과목 운영 시작 일자", example = "2026-06-01")
    LocalDate startAt,

    @Schema(description = "과목 운영 종료 일자", example = "2026-08-30")
    LocalDate endAt,

    @Schema(description = "과목의 정기 수업 요일", example = "MONDAY")
    DayOfWeek dayOfWeek,

    @Schema(description = "과목의 정기 수업 시작 시간", example = "19:20:00")
    LocalTime startTime,

    @Schema(description = "과목의 정기 수업 종료 시간", example = "20:00:00")
    LocalTime endTime,

    @Min(1)
    @Schema(description = "교시", example = "1")
    Integer period
) {}
