package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record StudentAttendanceSheetResponse(
    @Schema(description = "조회 연도", example = "2026")
    Integer year,

    @Schema(description = "조회 월", example = "2")
    Integer month,

    @Schema(description = "분반 ID", example = "1")
    Long classroomId,

    @Schema(description = "분반명", example = "벚꽃반")
    String classroomName,

    @Schema(description = "출석부 학생 목록")
    List<StudentAttendanceSheetStudentResponse> students,

    @Schema(description = "월간 수업 일정별 출석 목록")
    List<StudentAttendanceSheetScheduleResponse> schedules
) {

    public static StudentAttendanceSheetResponse of(
        Integer year,
        Integer month,
        Long classroomId,
        String classroomName,
        List<StudentAttendanceSheetStudentResponse> students,
        List<StudentAttendanceSheetScheduleResponse> schedules
    ) {
        return new StudentAttendanceSheetResponse(year, month, classroomId, classroomName, students, schedules);
    }
}
