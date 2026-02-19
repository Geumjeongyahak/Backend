package sonmoeum.domain.lesson.exception;

import sonmoeum.common.exception.DuplicateResourceException;
import sonmoeum.common.exception.ErrorCode;

public class LessonDuplicateException extends DuplicateResourceException {

    public LessonDuplicateException(String message) {
        super(ErrorCode.DUPLICATE_LESSON, message);
    }
}
