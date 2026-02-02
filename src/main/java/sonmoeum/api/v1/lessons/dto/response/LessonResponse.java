package sonmoeum.api.v1.lessons.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import sonmoeum.domain.lesson.entity.Lesson;

import io.swagger.v3.oas.annotations.media.Schema;

public record LessonResponse(
    @Schema(description = "수업 ID", example = "1")
    Long id,
    @Schema(description = "과목 ID", example = "1")
    Long subjectId,
    @Schema(description = "선생님 ID", example = "1")
    Long teacherId,
    @Schema(description = "날짜", example = "2024-03-04")
    LocalDate date,
    @Schema(description = "시작 시간", example = "14:00:00")
    LocalTime startTime,
    @Schema(description = "종료 시간", example = "16:00:00")
    LocalTime endTime,
    @Schema(description = "출석 상태", example = "PENDING")
    Lesson.AttendanceStatus attendance
) {
    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(
            lesson.getId(),
            lesson.getSubject().getId(),
            lesson.getTeacher().getId(),
            lesson.getDate(),
            lesson.getStartTime(),
            lesson.getEndTime(),
            lesson.getAttendance()
        );
    }
}
