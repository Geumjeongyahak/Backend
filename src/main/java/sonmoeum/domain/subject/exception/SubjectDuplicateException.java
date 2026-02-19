package sonmoeum.domain.subject.exception;

import sonmoeum.common.exception.DuplicateResourceException;
import sonmoeum.common.exception.ErrorCode;

public class SubjectDuplicateException extends DuplicateResourceException {

    public SubjectDuplicateException(String message) {
        super(ErrorCode.DUPLICATE_SUBJECT, message);
    }

}
