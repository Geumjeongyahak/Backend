package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record StudentAttendanceSheetScheduleResponse(
    @Schema(description = "하루 일정 식별자", example = "1")
    Long dailyScheduleId,

    @Schema(description = "수업 날짜", example = "2026-02-07")
    LocalDate lessonDate,

    @Schema(description = "월 중 날짜", example = "7")
    Integer day,

    @Schema(description = "요일", example = "토")
    String dayOfWeek,

    @Schema(
        description = "하루 일정 상태",
        example = "SCHEDULED",
        allowableValues = {"SCHEDULED", "COMPLETED", "CANCELLED"}
    )
    DailyScheduleStatus status,

    @Schema(description = "일정별 학생 출석 목록")
    List<StudentAttendanceSheetAttendanceResponse> studentAttendances
) {

    public static StudentAttendanceSheetScheduleResponse of(
        DailySchedule dailySchedule,
        String dayOfWeek,
        List<StudentAttendanceSheetAttendanceResponse> studentAttendances
    ) {
        return new StudentAttendanceSheetScheduleResponse(
            dailySchedule.getId(),
            dailySchedule.getLessonDate(),
            dailySchedule.getLessonDate().getDayOfMonth(),
            dayOfWeek,
            dailySchedule.getStatus(),
            studentAttendances
        );
    }
}
