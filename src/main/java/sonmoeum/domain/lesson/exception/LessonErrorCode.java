package sonmoeum.domain.lesson.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import sonmoeum.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum LessonErrorCode implements ErrorCode {
    LESSON_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-06-001", "수업을 찾을 수 없습니다."),
    STUDENT_NOT_ENROLLED(HttpStatus.NOT_FOUND, "RES-06-002", "수업에 등록된 학생이 아닙니다."),
    INVALID_LESSON_SCHEDULE(HttpStatus.BAD_REQUEST, "VAL-06-001", "수업 스케줄이 유효하지 않습니다."),
    DUPLICATE_LESSON(HttpStatus.CONFLICT, "BIZ-06-001", "이미 존재하는 수업입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
