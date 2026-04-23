package geumjeongyahak.domain.request.exception;

import geumjeongyahak.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RequestErrorCode implements ErrorCode {
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-07-001", "요청을 찾을 수 없습니다."),
    REQUEST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "BIZ-07-001", "이미 처리된 요청입니다."),
    REQUEST_FORBIDDEN(HttpStatus.FORBIDDEN, "REQ003", "해당 요청에 대한 권한이 없습니다."),
    INVALID_REQUEST_EXPIRES_IN_PAST(
        HttpStatus.BAD_REQUEST,
        "REQ-07-004",
        "만료 시각은 현재 시각 이후여야 합니다."
    ),
    INVALID_REQUEST_EXPIRES_AFTER_LESSON(
        HttpStatus.BAD_REQUEST,
        "REQ-07-005",
        "만료 시각은 수업일 이후일 수 없습니다."
    ),
    INVALID_REQUEST_EXPIRES_POLICY(
        HttpStatus.BAD_REQUEST,
        "REQ-07-006",
        "만료 시각은 수업일 3일 전까지 설정해야 합니다."
    ),
    DUPLICATE_ACTIVE_REQUEST(
        HttpStatus.CONFLICT,
        "REQ-07-007",
        "해당 수업에는 이미 진행 중인 교환 요청이 존재합니다."
    ),
    INVALID_REQUEST_LESSON_POLICY(
        HttpStatus.BAD_REQUEST,
        "REQ-07-008",
        "수업 교환 요청 가능 기간이 지났습니다."
    ),
    MULTIPLE_CLASSROOMS_IN_LESSON_EXCHANGE_REQUEST(
        HttpStatus.CONFLICT,
        "REQ-07-009",
        "하나의 수업 교환 요청에 여러 반이 포함될 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
