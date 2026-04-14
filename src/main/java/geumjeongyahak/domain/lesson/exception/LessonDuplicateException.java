package geumjeongyahak.domain.lesson.exception;

import geumjeongyahak.common.exception.DuplicateResourceException;

public class LessonDuplicateException extends DuplicateResourceException {

    public LessonDuplicateException(String message) {
        super(LessonErrorCode.DUPLICATE_LESSON, message);
    }
}
