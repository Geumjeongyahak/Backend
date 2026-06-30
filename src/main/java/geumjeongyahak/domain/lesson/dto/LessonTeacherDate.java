package geumjeongyahak.domain.lesson.dto;

import java.time.LocalDate;

public record LessonTeacherDate(
    Long teacherId,
    LocalDate date
) {
}
