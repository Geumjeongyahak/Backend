package geumjeongyahak.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RES001", "요청한 리소스를 찾을 수 없습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VAL001", "입력값 검증에 실패했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "VAL002", "잘못된 입력값입니다."),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "VAL003", "필수 필드가 누락되었습니다."),
    NO_CHANGES_DETECTED(HttpStatus.BAD_REQUEST, "VAL004", "변경된 값이 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "VAL007", "지원하지 않는 HTTP 메서드입니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "BIZ001", "이미 존재하는 리소스입니다."),
    INVALID_STATE(HttpStatus.CONFLICT, "BIZ004", "유효하지 않은 상태입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS001", "서버 내부 오류가 발생했습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS002", "데이터베이스 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS003", "외부 API 호출 중 오류가 발생했습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SYS004", "파일 업로드에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
