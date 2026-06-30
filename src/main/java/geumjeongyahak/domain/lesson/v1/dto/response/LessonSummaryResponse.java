package geumjeongyahak.domain.lesson.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.lesson.entity.Lesson;

public record LessonSummaryResponse(
    @Schema(description = "수업 식별자", example = "1")
    Long lessonId,

    @Schema(description = "수업 일자", example = "2026-03-01")
    LocalDate date,

    @Schema(description = "수업 교시", example = "1")
    Integer period,

    @Schema(description = "수업 시작 시간", example = "09:00")
    LocalTime startTime,

    @Schema(description = "수업 종료 시간", example = "10:00")
    LocalTime endTime,

    @Schema(description = "강사 이름", example = "홍길동")
    String teacherName,

    @Schema(description = "과목 이름", example = "수학")
    String subjectName,

    @Schema(description = "분반 식별자", example = "1")
    Long classroomId,

    @Schema(description = "분반 이름", example = "벚꽃반")
    String classroomName,

    @Schema(description = "교환 또는 대체 수업 여부", example = "true")
    boolean isExchanged,

    @Schema(description = "승인된 결석 요청에 따른 결강 여부", example = "false")
    boolean isAbsent,

    @Schema(
        description = "교환 상대 수업 날짜. 대체 수업이거나 교환되지 않은 수업이면 null입니다.",
        example = "2026-06-26",
        nullable = true
    )
    LocalDate exchangedLessonDate,

    @Schema(description = "같은 날짜/분반 DailySchedule 기준 교사 출석/퇴근 여부. 아직 출석 정보가 없으면 null입니다.")
    LessonTeacherAttendanceResponse teacherAttendance
) {

    public static LessonSummaryResponse from(Lesson lesson) {
        return from(lesson, false, false, null, null);
    }

    public static LessonSummaryResponse from(
        Lesson lesson,
        boolean isExchanged,
        boolean isAbsent,
        LocalDate exchangedLessonDate,
        DailyTeacherAttendance teacherAttendance
    ) {
        return new LessonSummaryResponse(
            lesson.getId(),
            lesson.getDate(),
            lesson.getPeriod(),
            lesson.getStartTime(),
            lesson.getEndTime(),
            lesson.getTeacher().getName(),
            lesson.getSubject().getName(),
            lesson.getSubject().getClassroom().getId(),
            lesson.getSubject().getClassroom().getName(),
            isExchanged,
            isAbsent,
            exchangedLessonDate,
            LessonTeacherAttendanceResponse.from(teacherAttendance)
        );
    }
}
