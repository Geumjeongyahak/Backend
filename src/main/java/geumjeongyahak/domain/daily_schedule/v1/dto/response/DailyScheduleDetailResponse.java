package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.lesson.entity.Lesson;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DailyScheduleDetailResponse(
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

    @Schema(description = "담당 교사 연락처", example = "010-0000-0000")
    String teacherPhoneNumber,

    @Schema(description = "주민번호 앞자리", example = "900101")
    String residentRegistrationNumberPrefix,

    @Schema(description = "개인정보 활용 동의 여부", example = "true")
    boolean personalInfoConsent,

    @Schema(description = "활동 시작 시간", example = "14:00:00")
    LocalTime activityStartTime,

    @Schema(description = "활동 종료 시간", example = "16:00:00")
    LocalTime activityEndTime,

    @Schema(description = "하루 일정 상태", example = "SCHEDULED")
    DailyScheduleStatus status,

    @Schema(description = "교사 출석 정보")
    DailyTeacherAttendanceResponse teacherAttendance,

    @Schema(description = "하루 일정에 연결된 교시별 수업 목록")
    List<DailyScheduleLessonResponse> lessons,

    @Schema(description = "하루 일정 학생 출석부")
    List<DailyStudentAttendanceResponse> studentAttendances
) {

    public static DailyScheduleDetailResponse of(
        DailySchedule dailySchedule,
        DailyTeacherAttendance teacherAttendance,
        List<Lesson> lessons,
        List<DailyStudentAttendance> studentAttendances,
        boolean includeSensitiveInfo
    ) {
        return new DailyScheduleDetailResponse(
            dailySchedule.getId(),
            dailySchedule.getLessonDate(),
            dailySchedule.getClassroom().getId(),
            dailySchedule.getClassroom().getName(),
            dailySchedule.getTeacher().getId(),
            dailySchedule.getTeacher().getName(),
            includeSensitiveInfo ? dailySchedule.getTeacher().getPhoneNumber() : null,
            includeSensitiveInfo ? dailySchedule.getResidentRegistrationNumberPrefix() : null,
            dailySchedule.isPersonalInfoConsent(),
            dailySchedule.getActivityStartTime(),
            dailySchedule.getActivityEndTime(),
            dailySchedule.getStatus(),
            DailyTeacherAttendanceResponse.from(teacherAttendance),
            lessons.stream().map(DailyScheduleLessonResponse::from).toList(),
            studentAttendances.stream().map(DailyStudentAttendanceResponse::from).toList()
        );
    }
}
