package geumjeongyahak.domain.lesson.v1.dto.response;

import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import io.swagger.v3.oas.annotations.media.Schema;

public record LessonTeacherAttendanceResponse(
    @Schema(description = "교사 출석 처리 여부", example = "true")
    boolean isAttended,

    @Schema(description = "교사 퇴근 처리 여부", example = "false")
    boolean isCheckedOut
) {

    public static LessonTeacherAttendanceResponse from(DailyTeacherAttendance teacherAttendance) {
        if (teacherAttendance == null) {
            return null;
        }

        return new LessonTeacherAttendanceResponse(
            teacherAttendance.isAttended(),
            teacherAttendance.isCheckedOut()
        );
    }
}
