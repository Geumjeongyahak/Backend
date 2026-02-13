package sonmoeum.domain.lesson.exception;

import sonmoeum.common.exception.ErrorCode;
import sonmoeum.common.exception.ResourceNotFoundException;

public class LessonNotFoundException extends ResourceNotFoundException {

    public LessonNotFoundException(Long lessonId) {
        super(ErrorCode.LESSON_NOT_FOUND, lessonId);
    }
}
