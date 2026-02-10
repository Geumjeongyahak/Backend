package sonmoeum.domain.auth.exception;

import sonmoeum.common.exception.AuthenticationException;
import sonmoeum.common.exception.ErrorCode;

/**
 * Refresh Token 검증 실패 예외
 */
public class InvalidRefreshTokenException extends AuthenticationException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    public InvalidRefreshTokenException(String message) {
        super(ErrorCode.INVALID_TOKEN, message);
    }
}
