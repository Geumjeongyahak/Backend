package sonmoeum.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 에러 코드 정의
 * - HTTP 상태 코드와 비즈니스 에러 코드를 매핑
 * - 일관된 에러 응답 제공
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ============ 인증 관련 (401 Unauthorized) ============
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH001", "인증에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH004", "Refresh Token을 찾을 수 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH005", "아이디 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND_FOR_AUTH(HttpStatus.UNAUTHORIZED, "AUTH006", "사용자를 찾을 수 없습니다."),

    // ============ 인가 관련 (403 Forbidden) ============
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTHZ001", "접근 권한이 없습니다."),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "AUTHZ002", "권한이 부족합니다."),

    // ============ 리소스 관련 (404 Not Found) ============
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RES001", "요청한 리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "RES002", "사용자를 찾을 수 없습니다."),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "RES003", "역할을 찾을 수 없습니다."),

    // ============ 요청 검증 관련 (400 Bad Request) ============
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VAL001", "입력값 검증에 실패했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "VAL002", "잘못된 입력값입니다."),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "VAL003", "필수 필드가 누락되었습니다."),

    // ============ 비즈니스 로직 관련 (409 Conflict) ============
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "BIZ001", "이미 존재하는 리소스입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "BIZ002", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "BIZ003", "이미 사용 중인 이메일입니다."),
    INVALID_STATE(HttpStatus.CONFLICT, "BIZ004", "유효하지 않은 상태입니다."),

    // ============ Role 관련 비즈니스 로직 ============
    CANNOT_ASSIGN_BASE_ROLE(HttpStatus.BAD_REQUEST, "ROLE001", "기본 역할은 직접 부여할 수 없습니다."),
    CANNOT_REMOVE_BASE_ROLE(HttpStatus.BAD_REQUEST, "ROLE002", "기본 역할은 직접 제거할 수 없습니다."),
    ROLE_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "ROLE003", "사용자에게 이미 해당 역할이 부여되어 있습니다."),
    ROLE_NOT_ASSIGNED(HttpStatus.BAD_REQUEST, "ROLE004", "사용자에게 해당 역할이 부여되어 있지 않습니다."),

    // ============ 서버 에러 (500 Internal Server Error) ============
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS001", "서버 내부 오류가 발생했습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS002", "데이터베이스 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS003", "외부 API 호출 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    /**
     * ErrorCode로부터 BusinessException 생성
     */
    public BusinessException toException() {
        return new BusinessException(this.message, this.status, this.code);
    }

    /**
     * 커스텀 메시지로 BusinessException 생성
     */
    public BusinessException toException(String customMessage) {
        return new BusinessException(customMessage, this.status, this.code);
    }

    /**
     * 원인 예외와 함께 BusinessException 생성
     */
    public BusinessException toException(Throwable cause) {
        return new BusinessException(this.message, this.status, this.code, cause);
    }

    /**
     * 커스텀 메시지와 원인 예외를 함께 BusinessException 생성
     */
    public BusinessException toException(String customMessage, Throwable cause) {
        return new BusinessException(customMessage, this.status, this.code, cause);
    }
}
