package sonmoeum.domain.subject.exception;

import sonmoeum.common.exception.DuplicateResourceException;

public class SubjectDuplicateException extends DuplicateResourceException {

    public SubjectDuplicateException(String message) {
        super(SubjectErrorCode.DUPLICATE_SUBJECT, message);
    }

}
