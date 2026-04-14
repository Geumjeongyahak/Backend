package geumjeongyahak.common.exception;

/**
 * 잘못된 요청 예외 (400 Bad Request)
 * - 비즈니스 규칙/관계 검증(기간/시간/상태 등)에서 400을 내려야 할 때 사용
 */
public class BadRequestException extends BusinessException {

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatus(), errorCode.getCode());
    }

    public BadRequestException(ErrorCode errorCode, String customMessage) {
        super(customMessage, errorCode.getStatus(), errorCode.getCode());
    }
}
