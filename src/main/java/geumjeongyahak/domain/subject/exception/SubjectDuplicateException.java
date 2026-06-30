package geumjeongyahak.domain.subject.exception;

import geumjeongyahak.common.exception.DuplicateResourceException;

public class SubjectDuplicateException extends DuplicateResourceException {

    public SubjectDuplicateException(String message) {
        super(SubjectErrorCode.DUPLICATE_SUBJECT, message);
    }

}
