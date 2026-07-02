package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import geumjeongyahak.domain.student.entity.Student;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudentAttendanceSheetStudentResponse(
    @Schema(description = "학생 ID", example = "10")
    Long studentId,

    @Schema(description = "학생 이름", example = "김민수")
    String studentName
) {

    public static StudentAttendanceSheetStudentResponse from(Student student) {
        return new StudentAttendanceSheetStudentResponse(
            student.getId(),
            student.getName()
        );
    }
}
