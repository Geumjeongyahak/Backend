package geumjeongyahak.domain.classroom.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import geumjeongyahak.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ClassroomErrorCode implements ErrorCode {
    CLASSROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-03-001", "분반을 찾을 수 없습니다."),
    DUPLICATE_CLASSROOM(HttpStatus.CONFLICT, "BIZ-03-001", "이미 존재하는 분반입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
