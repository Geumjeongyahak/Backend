package geumjeongyahak.domain.auth.exception;

import geumjeongyahak.common.exception.BusinessException;

public class CredentialNotFoundException extends BusinessException {
    public CredentialNotFoundException() {
        super(AuthErrorCode.CREDENTIAL_NOT_FOUND);
    }
    
}
