package geumjeongyahak.domain.student.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import geumjeongyahak.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum StudentErrorCode implements ErrorCode {
    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-04-001", "학생을 찾을 수 없습니다."),
    DUPLICATE_STUDENT(HttpStatus.CONFLICT, "BIZ-04-001", "이미 존재하는 학생입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
