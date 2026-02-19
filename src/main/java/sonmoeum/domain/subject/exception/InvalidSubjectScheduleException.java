package sonmoeum.domain.subject.exception;

import sonmoeum.common.exception.BadRequestException;
import sonmoeum.common.exception.ErrorCode;

public class InvalidSubjectScheduleException extends BadRequestException {

    public InvalidSubjectScheduleException(String customMessage) {
        super(ErrorCode.INVALID_SUBJECT_SCHEDULE, customMessage);
    }
}
