package geumjeongyahak.domain.auth.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DuplicateCredentialException extends BusinessException {
    public DuplicateCredentialException() {
        super(AuthErrorCode.DUPLICATED_CREDENTIAL);
    }

    public DuplicateCredentialException(String message) {
        super(AuthErrorCode.DUPLICATED_CREDENTIAL, message);
    }
}
