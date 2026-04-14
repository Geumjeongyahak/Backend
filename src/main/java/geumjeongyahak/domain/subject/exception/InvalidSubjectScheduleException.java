package geumjeongyahak.domain.subject.exception;

import geumjeongyahak.common.exception.BadRequestException;

public class InvalidSubjectScheduleException extends BadRequestException {

    public InvalidSubjectScheduleException(String customMessage) {
        super(SubjectErrorCode.INVALID_SUBJECT_SCHEDULE, customMessage);
    }
}
