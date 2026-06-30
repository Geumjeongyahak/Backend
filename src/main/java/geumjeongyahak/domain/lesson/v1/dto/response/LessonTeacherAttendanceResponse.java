package geumjeongyahak.domain.lesson.v1.dto.response;

import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record LessonTeacherAttendanceResponse(
    @Schema(description = "교사 출석 처리 시각. 출석 처리 전이면 null입니다.", example = "2027-05-21T19:20:00")
    LocalDateTime attendedAt,

    @Schema(description = "교사 출석 처리 여부", example = "true")
    boolean isAttended,

    @Schema(description = "교사 퇴근 처리 시각. 퇴근 처리 전이면 null입니다.", example = "2027-05-21T20:00:00")
    LocalDateTime checkedOutAt,

    @Schema(description = "교사 퇴근 처리 여부", example = "false")
    boolean isCheckedOut
) {

    public static LessonTeacherAttendanceResponse from(DailyTeacherAttendance teacherAttendance) {
        if (teacherAttendance == null) {
            return null;
        }

        return new LessonTeacherAttendanceResponse(
            teacherAttendance.getAttendedAt(),
            teacherAttendance.isAttended(),
            teacherAttendance.getCheckedOutAt(),
            teacherAttendance.isCheckedOut()
        );
    }
}
