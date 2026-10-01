package geumjeongyahak.domain.subject.exception;

import geumjeongyahak.common.exception.BusinessException;

public class SubjectNotCopyableException extends BusinessException {

    public SubjectNotCopyableException(String customMessage) {
        super(SubjectErrorCode.SUBJECT_NOT_COPYABLE, customMessage);
    }
}
