package sonmoeum.domain.lesson.exception;

import sonmoeum.common.exception.DuplicateResourceException;

public class LessonDuplicateException extends DuplicateResourceException {

    public LessonDuplicateException(String message) {
        super(LessonErrorCode.DUPLICATE_LESSON, message);
    }
}
