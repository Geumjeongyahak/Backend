package geumjeongyahak.domain.lesson.exception;

import geumjeongyahak.common.exception.ResourceNotFoundException;

public class LessonNotFoundException extends ResourceNotFoundException {

    public LessonNotFoundException(Long lessonId) {
        super(LessonErrorCode.LESSON_NOT_FOUND, lessonId);
    }
}
