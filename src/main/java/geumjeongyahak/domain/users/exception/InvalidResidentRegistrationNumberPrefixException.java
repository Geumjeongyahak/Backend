package geumjeongyahak.domain.users.exception;

import geumjeongyahak.common.exception.BusinessException;

public class InvalidResidentRegistrationNumberPrefixException extends BusinessException {

    public InvalidResidentRegistrationNumberPrefixException() {
        super(UserErrorCode.INVALID_RESIDENT_REGISTRATION_NUMBER_PREFIX);
    }
}
