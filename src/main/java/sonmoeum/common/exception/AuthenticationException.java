package sonmoeum.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증 실패 예외 (401 Unauthorized)
 * - 로그인 실패
 * - 토큰 검증 실패
 * - 인증 정보 누락
 */
public class AuthenticationException extends BusinessException {

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatus(), errorCode.getCode());
    }

    public AuthenticationException(ErrorCode errorCode, String customMessage) {
        super(customMessage, errorCode.getStatus(), errorCode.getCode());
    }

    public AuthenticationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), errorCode.getStatus(), errorCode.getCode(), cause);
    }

    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTH999");
    }
}
