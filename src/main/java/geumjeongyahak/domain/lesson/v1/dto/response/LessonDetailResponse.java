package geumjeongyahak.domain.lesson.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.enums.TeacherAttendanceStatus;

public record LessonDetailResponse(
    @Schema(description = "수업 식별자", example = "1")
    Long lessonId,

    @Schema(description = "수업 일자", example = "2026-02-13")
    LocalDate date,

    @Schema(description = "수업 교시", example = "1")
    Integer period,

    @Schema(description = "수업 시작 시간", example = "19:20")
    LocalTime startTime,

    @Schema(description = "수업 종료 시간", example = "20:00")
    LocalTime endTime,

    @Schema(description = "수업 상태", example = "SCHEDULED")
    LessonStatus status,

    @Schema(description = "교사 출석 상태", example = "ABSENT")
    TeacherAttendanceStatus teacherAttendance,

    @Schema(description = "강사 이름", example = "홍길동")
    String teacherName,

    @Schema(description = "과목 이름", example = "한글 기초")
    String subjectName,

    @Schema(description = "수업일지(메모)")
    String note
) {
    public static LessonDetailResponse from(Lesson lesson) {
        return new LessonDetailResponse(
            lesson.getId(),
            lesson.getDate(),
            lesson.getPeriod(),
            lesson.getStartTime(),
            lesson.getEndTime(),
            lesson.getStatus(),
            lesson.getTeacherAttendance(),
            lesson.getTeacher().getName(),
            lesson.getSubject().getName(),
            lesson.getNote()
        );
    }
}
