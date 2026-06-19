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

    @Schema(description = "담당 교사 연락처. 민감 정보 조회 권한이 없으면 null입니다.", example = "010-0000-0000")
    String teacherPhoneNumber,

    @Schema(description = "주민번호 앞자리. 민감 정보 조회 권한이 없으면 null입니다.", example = "900101")
    String residentRegistrationNumberPrefix,

    @Schema(description = "개인정보 활용 동의 여부", example = "true")
    boolean personalInfoConsent,

    @Schema(description = "활동 시작 시간", example = "14:00:00")
    LocalTime activityStartTime,

    @Schema(description = "활동 종료 시간", example = "16:00:00")
    LocalTime activityEndTime,

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

    @Schema(description = "교사 출석 정보. 아직 출석 정보가 생성되지 않은 경우 null입니다.")
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
            dailySchedule.isExchanged(),
            dailySchedule.isAbsent(),
            dailySchedule.getExchangedLessonDate(),
            DailyTeacherAttendanceResponse.from(teacherAttendance),
            lessons.stream().map(DailyScheduleLessonResponse::from).toList(),
            studentAttendances.stream().map(DailyStudentAttendanceResponse::from).toList()
        );
    }
}
