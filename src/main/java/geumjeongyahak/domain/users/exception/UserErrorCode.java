package geumjeongyahak.domain.users.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import geumjeongyahak.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-01-001", "사용자를 찾을 수 없습니다."),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-01-002", "역할을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "BIZ-01-002", "이미 사용 중인 이메일입니다."),
    ROLE_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "BIZ-01-003", "사용자에게 이미 해당 역할이 부여되어 있습니다."),
    ROLE_NOT_ASSIGNED(HttpStatus.BAD_REQUEST, "VAL-01-001", "사용자에게 해당 역할이 부여되어 있지 않습니다."),
    CANNOT_DELETE_ROLE_IN_USE(HttpStatus.BAD_REQUEST, "BIZ-01-004", "해당 역할이 사용자에게 할당되어 있어 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
