package sonmoeum.domain.lesson.exception;

import sonmoeum.common.exception.ResourceNotFoundException;

public class LessonNotFoundException extends ResourceNotFoundException {

    public LessonNotFoundException(Long lessonId) {
        super(LessonErrorCode.LESSON_NOT_FOUND, lessonId);
    }
}
