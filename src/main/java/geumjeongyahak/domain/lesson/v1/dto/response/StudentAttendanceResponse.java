package geumjeongyahak.domain.lesson.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.lesson.entity.StudentAttendance;
import geumjeongyahak.domain.lesson.enums.StudentAttendanceStatus;

public record StudentAttendanceResponse(
    @Schema(description = "학생 ID", example = "1")
    Long studentId,

    @Schema(description = "학생 이름", example = "이영희")
    String studentName,

    @Schema(description = "출석 상태", example = "ABSENT")
    StudentAttendanceStatus status,

    @Schema(description = "메모", example = "지각 예정")
    String memo
) {
    public static StudentAttendanceResponse from(StudentAttendance attendance) {
        return new StudentAttendanceResponse(
            attendance.getStudent().getId(),
            attendance.getStudent().getName(),
            attendance.getStatus(),
            attendance.getMemo()
        );
    }
}