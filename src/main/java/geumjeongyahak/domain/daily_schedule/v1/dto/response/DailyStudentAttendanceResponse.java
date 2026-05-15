package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyStudentAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record DailyStudentAttendanceResponse(
    @Schema(description = "학생 출석 식별자", example = "1")
    Long attendanceId,

    @Schema(description = "학생 ID", example = "10")
    Long studentId,

    @Schema(description = "학생 이름", example = "최양지")
    String studentName,

    @Schema(description = "학생 출석 상태", example = "ABSENT")
    DailyStudentAttendanceStatus status
) {

    public static DailyStudentAttendanceResponse from(DailyStudentAttendance attendance) {
        return new DailyStudentAttendanceResponse(
            attendance.getId(),
            attendance.getStudent().getId(),
            attendance.getStudent().getName(),
            attendance.getStatus()
        );
    }
}
