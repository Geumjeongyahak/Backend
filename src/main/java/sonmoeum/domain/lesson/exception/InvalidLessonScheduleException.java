package sonmoeum.domain.lesson.exception;

import sonmoeum.common.exception.BadRequestException;

public class InvalidLessonScheduleException extends BadRequestException {

    public InvalidLessonScheduleException(String customMessage) {
        super(LessonErrorCode.INVALID_LESSON_SCHEDULE, customMessage);
    }
}
