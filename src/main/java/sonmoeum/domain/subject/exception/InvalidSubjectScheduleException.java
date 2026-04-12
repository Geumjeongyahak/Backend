package sonmoeum.domain.subject.exception;

import sonmoeum.common.exception.BadRequestException;

public class InvalidSubjectScheduleException extends BadRequestException {

    public InvalidSubjectScheduleException(String customMessage) {
        super(SubjectErrorCode.INVALID_SUBJECT_SCHEDULE, customMessage);
    }
}
