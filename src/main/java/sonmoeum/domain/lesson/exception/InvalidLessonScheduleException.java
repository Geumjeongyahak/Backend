package sonmoeum.domain.lesson.exception;

import sonmoeum.common.exception.BadRequestException;
import sonmoeum.common.exception.ErrorCode;

public class InvalidLessonScheduleException extends BadRequestException {

    public InvalidLessonScheduleException(String customMessage) {
        super(ErrorCode.INVALID_LESSON_SCHEDULE, customMessage);
    }
}
