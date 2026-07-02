package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyStudentAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudentAttendanceSheetAttendanceResponse(
    @Schema(description = "학생 출석 식별자", example = "1")
    Long attendanceId,

    @Schema(description = "학생 ID", example = "10")
    Long studentId,

    @Schema(
        description = "학생 출석 상태",
        example = "PRESENT",
        allowableValues = {"PRESENT", "ABSENT", "LATE"}
    )
    DailyStudentAttendanceStatus status
) {

    public static StudentAttendanceSheetAttendanceResponse from(DailyStudentAttendance attendance) {
        return new StudentAttendanceSheetAttendanceResponse(
            attendance.getId(),
            attendance.getStudent().getId(),
            attendance.getStatus()
        );
    }
}
