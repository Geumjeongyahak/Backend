package geumjeongyahak.domain.subject.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import geumjeongyahak.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum SubjectErrorCode implements ErrorCode {
    SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-05-001", "과목을 찾을 수 없습니다."),
    INVALID_SUBJECT_SCHEDULE(HttpStatus.BAD_REQUEST, "VAL-05-001", "과목 스케줄이 유효하지 않습니다."),
    DUPLICATE_SUBJECT(HttpStatus.CONFLICT, "BIZ-05-001", "이미 존재하는 과목입니다."),
    SUBJECT_TEACHER_ASSIGNMENT_CONFLICT(
        HttpStatus.CONFLICT,
        "BIZ-05-002",
        "담당 교사를 자동 배정할 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
