package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DailyScheduleSummaryResponse(
    @Schema(description = "하루 일정 식별자", example = "1")
    Long dailyScheduleId,

    @Schema(description = "수업 날짜", example = "2026-06-20")
    LocalDate lessonDate,

    @Schema(description = "분반 ID", example = "1")
    Long classroomId,

    @Schema(description = "분반명", example = "장미반")
    String classroomName,

    @Schema(description = "담당 교사 ID", example = "2")
    Long teacherId,

    @Schema(description = "담당 교사명", example = "홍길동")
    String teacherName,

    @Schema(description = "활동 시작 시간", example = "14:00:00")
    LocalTime activityStartTime,

    @Schema(description = "활동 종료 시간", example = "16:00:00")
    LocalTime activityEndTime,

    @Schema(description = "봉사 인정 시간(분)", example = "120")
    Integer volunteerServiceMinutes,

    @Schema(
        description = "하루 일정 상태",
        example = "SCHEDULED",
        allowableValues = {"SCHEDULED", "COMPLETED", "CANCELLED"}
    )
    DailyScheduleStatus status,

    @Schema(description = "교환 또는 대체 수업 여부", example = "true")
    boolean isExchanged,

    @Schema(description = "승인된 결석 요청에 따른 결강 여부", example = "false")
    boolean isAbsent,

    @Schema(
        description = "교환 상대 수업 날짜. 대체 수업이거나 교환되지 않은 일정이면 null입니다.",
        example = "2026-06-26",
        nullable = true
    )
    LocalDate exchangedLessonDate,

    @Schema(
        description = "교사 출석 상태",
        example = "ABSENT",
        allowableValues = {"PRESENT", "ABSENT", "LATE", "EXCUSED"}
    )
    DailyTeacherAttendanceStatus teacherAttendanceStatus,

    @Schema(description = "연결된 수업 수", example = "3")
    int lessonCount,

    @Schema(description = "하루 일정에 연결된 교시별 수업 일지 요약")
    List<DailyScheduleLessonResponse> lessons
) {

    public static DailyScheduleSummaryResponse of(
        DailySchedule dailySchedule,
        DailyTeacherAttendance teacherAttendance,
        List<DailyScheduleLessonResponse> lessons
    ) {
        return new DailyScheduleSummaryResponse(
            dailySchedule.getId(),
            dailySchedule.getLessonDate(),
            dailySchedule.getClassroom().getId(),
            dailySchedule.getClassroom().getName(),
            dailySchedule.getTeacher().getId(),
            dailySchedule.getTeacher().getName(),
            dailySchedule.getActivityStartTime(),
            dailySchedule.getActivityEndTime(),
            teacherAttendance != null ? teacherAttendance.getVolunteerServiceMinutes() : null,
            dailySchedule.getStatus(),
            dailySchedule.isExchanged(),
            dailySchedule.isAbsent(),
            dailySchedule.getExchangedLessonDate(),
            teacherAttendance != null ? teacherAttendance.getStatus() : null,
            lessons.size(),
            lessons
        );
    }
}
