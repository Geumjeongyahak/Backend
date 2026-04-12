package sonmoeum.domain.request.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import sonmoeum.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum RequestErrorCode implements ErrorCode {
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-07-001", "요청을 찾을 수 없습니다."),
    REQUEST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "BIZ-07-001", "이미 처리된 요청입니다."),
    REQUEST_FORBIDDEN(HttpStatus.FORBIDDEN, "REQ003", "해당 요청에 대한 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
