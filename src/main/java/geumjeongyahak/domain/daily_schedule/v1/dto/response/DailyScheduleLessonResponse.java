package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import geumjeongyahak.domain.lesson.entity.Lesson;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

public record DailyScheduleLessonResponse(
    @Schema(description = "수업 식별자", example = "1")
    Long lessonId,

    @Schema(description = "교시", example = "1")
    Integer period,

    @Schema(description = "수업 시작 시간", example = "14:00:00")
    LocalTime startTime,

    @Schema(description = "수업 종료 시간", example = "15:00:00")
    LocalTime endTime,

    @Schema(description = "과목명", example = "국어")
    String subjectName,

    @Schema(description = "교시별 수업 일지 내용")
    String note
) {

    public static DailyScheduleLessonResponse from(Lesson lesson) {
        return new DailyScheduleLessonResponse(
            lesson.getId(),
            lesson.getPeriod(),
            lesson.getStartTime(),
            lesson.getEndTime(),
            lesson.getSubject().getName(),
            lesson.getNote()
        );
    }
}
