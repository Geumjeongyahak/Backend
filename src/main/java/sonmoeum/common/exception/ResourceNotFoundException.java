package sonmoeum.common.exception;

/**
 * 리소스를 찾을 수 없는 예외 (404 Not Found)
 * - 존재하지 않는 사용자
 * - 존재하지 않는 데이터
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatus(), errorCode.getCode());
    }

    public ResourceNotFoundException(ErrorCode errorCode, String customMessage) {
        super(customMessage, errorCode.getStatus(), errorCode.getCode());
    }

    public ResourceNotFoundException(ErrorCode errorCode, Long id) {
        super(errorCode.getMessage() + " (ID: " + id + ")", errorCode.getStatus(), errorCode.getCode());
    }
}
