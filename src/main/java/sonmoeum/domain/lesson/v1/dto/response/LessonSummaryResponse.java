package sonmoeum.domain.lesson.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import sonmoeum.domain.lesson.entity.Lesson;

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
    String subjectName
) {

    public static LessonSummaryResponse from(Lesson lesson) {
        return new LessonSummaryResponse(
            lesson.getId(),
            lesson.getDate(),
            lesson.getPeriod(),
            lesson.getStartTime(),
            lesson.getEndTime(),
            lesson.getTeacher().getName(),
            lesson.getSubject().getName()
        );
    }
}
