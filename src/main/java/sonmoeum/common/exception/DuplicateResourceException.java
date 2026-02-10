package sonmoeum.common.exception;

/**
 * 중복된 리소스 예외 (409 Conflict)
 * - 이미 존재하는 사용자명
 * - 이미 존재하는 이메일
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(ErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatus(), errorCode.getCode());
    }

    public DuplicateResourceException(ErrorCode errorCode, String customMessage) {
        super(customMessage, errorCode.getStatus(), errorCode.getCode());
    }

    public DuplicateResourceException(ErrorCode errorCode, String resourceType, String value) {
        super(errorCode.getMessage() + " (" + resourceType + ": " + value + ")",
                errorCode.getStatus(), errorCode.getCode());
    }
}
