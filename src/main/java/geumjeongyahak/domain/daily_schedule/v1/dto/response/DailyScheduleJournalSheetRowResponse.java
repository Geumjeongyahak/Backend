package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.lesson.entity.Lesson;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record DailyScheduleJournalSheetRowResponse(
    @Schema(description = "하루 일정 식별자", example = "1")
    Long dailyScheduleId,

    @Schema(description = "분반 식별자", example = "1")
    Long classroomId,

    @Schema(description = "분반명", example = "장미반")
    String classroomName,

    @Schema(description = "담당 교사 식별자", example = "2")
    Long teacherId,

    @Schema(description = "담당 교사명", example = "홍길동")
    String teacherName,

    @Schema(description = "담당 교사 연락처", example = "010-1234-5678", nullable = true)
    String teacherPhoneNumber,

    @Schema(description = "주민등록번호 앞자리", example = "900101", nullable = true)
    String residentRegistrationNumberPrefix,

    @Schema(description = "수업 날짜", example = "2026-07-15")
    LocalDate lessonDate,

    @Schema(description = "활동 시작 시간", example = "14:00:00", nullable = true)
    LocalTime activityStartTime,

    @Schema(description = "활동 종료 시간", example = "16:00:00", nullable = true)
    LocalTime activityEndTime,

    @Schema(description = "실제 출근 시각. 출근 기록이 없으면 null입니다.", example = "2026-07-15T13:55:00", nullable = true)
    LocalDateTime attendedAt,

    @Schema(description = "실제 퇴근 시각. 퇴근 기록이 없으면 null입니다.", example = "2026-07-15T16:05:00", nullable = true)
    LocalDateTime checkedOutAt,

    @Schema(description = "하루 일정 상태", example = "COMPLETED")
    DailyScheduleStatus status,

    @Schema(description = "교사 출석 상태", example = "PRESENT", nullable = true)
    DailyTeacherAttendanceStatus teacherAttendanceStatus,

    @Schema(description = "개인정보 활용 동의 여부", example = "true")
    boolean personalInfoConsent,

    @Schema(description = "교시별 수업 일지")
    List<DailyScheduleLessonResponse> lessons
) {

    public static DailyScheduleJournalSheetRowResponse of(
        DailySchedule dailySchedule,
        DailyTeacherAttendance teacherAttendance,
        List<Lesson> lessons
    ) {
        return new DailyScheduleJournalSheetRowResponse(
            dailySchedule.getId(),
            dailySchedule.getClassroom().getId(),
            dailySchedule.getClassroom().getName(),
            dailySchedule.getTeacher().getId(),
            dailySchedule.getTeacher().getName(),
            dailySchedule.getTeacher().getPhoneNumber(),
            resolveResidentRegistrationNumberPrefix(dailySchedule),
            dailySchedule.getLessonDate(),
            dailySchedule.getActivityStartTime(),
            dailySchedule.getActivityEndTime(),
            teacherAttendance != null ? teacherAttendance.getAttendedAt() : null,
            teacherAttendance != null ? teacherAttendance.getCheckedOutAt() : null,
            dailySchedule.getStatus(),
            teacherAttendance != null ? teacherAttendance.getStatus() : null,
            dailySchedule.isPersonalInfoConsent(),
            lessons.stream().map(DailyScheduleLessonResponse::from).toList()
        );
    }

    private static String resolveResidentRegistrationNumberPrefix(DailySchedule dailySchedule) {
        String journalValue = dailySchedule.getResidentRegistrationNumberPrefix();
        if (journalValue != null && !journalValue.isBlank()) {
            return journalValue;
        }
        return dailySchedule.getTeacher().getResidentRegistrationNumberPrefix();
    }
}
