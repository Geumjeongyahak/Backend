package sonmoeum.domain.auth.exception;

import sonmoeum.common.exception.AuthenticationException;

/**
 * Refresh Token 검증 실패 예외
 */
public class InvalidRefreshTokenException extends AuthenticationException {

    public InvalidRefreshTokenException() {
        super(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    public InvalidRefreshTokenException(String message) {
        super(AuthErrorCode.INVALID_TOKEN, message);
    }
}
