package sonmoeum.domain.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import sonmoeum.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH001", "인증에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH004", "Refresh Token을 찾을 수 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH005", "아이디 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND_FOR_AUTH(HttpStatus.UNAUTHORIZED, "AUTH006", "사용자를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTHZ001", "접근 권한이 없습니다."),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "AUTHZ002", "권한이 부족합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
