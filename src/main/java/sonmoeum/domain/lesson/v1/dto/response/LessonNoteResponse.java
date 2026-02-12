package sonmoeum.domain.lesson.v1.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import sonmoeum.domain.lesson.entity.Lesson;

public record LessonNoteResponse(
    @Schema(description = "수업 식별자", example = "1")
    Long lessonId,

    @Schema(description = "수업일지(메모)")
    String note
) {
    public static LessonNoteResponse from(Lesson lesson) {
        return new LessonNoteResponse(lesson.getId(), lesson.getNote());
    }
}
