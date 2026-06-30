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
    DUPLICATE_ACTIVE_ABSENCE_REQUEST(
        HttpStatus.CONFLICT,
        "REQ-07-022",
        "해당 수업에는 이미 진행 중인 결석 요청이 존재합니다."
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
    ),
    REQUEST_NOT_PROPOSABLE(
        HttpStatus.CONFLICT,
        "REQ-07-010",
        "현재 상태의 요청에는 제안할 수 없습니다."
    ),
    REQUEST_EXPIRED_FOR_PROPOSAL(
        HttpStatus.CONFLICT,
        "REQ-07-011",
        "제안 가능 시간이 지난 요청입니다."
    ),
    CANNOT_PROPOSE_TO_OWN_REQUEST(
        HttpStatus.FORBIDDEN,
        "REQ-07-012",
        "자신의 수업 교환 요청에는 제안할 수 없습니다."
    ),
    DUPLICATE_ACTIVE_PROPOSAL(
        HttpStatus.CONFLICT,
        "REQ-07-013",
        "이미 활성 상태의 제안을 작성했습니다."
    ),
    PROPOSAL_TIME_OVERLAPS_REQUEST(
        HttpStatus.BAD_REQUEST,
        "REQ-07-014",
        "요청 수업과 같은 시간대의 수업은 제안할 수 없습니다."
    ),
    PROPOSAL_LESSONS_NOT_FOUND(
        HttpStatus.BAD_REQUEST,
        "REQ-07-015",
        "제안 조건에 맞는 수업을 찾을 수 없습니다."
    ),
    PROPOSAL_SCHEDULE_CONFLICT(
        HttpStatus.CONFLICT,
        "REQ-07-016",
        "해당 시간대에 이미 다른 수업이 있어 제안할 수 없습니다."
    ),
    MULTIPLE_CLASSROOMS_IN_LESSON_EXCHANGE_PROPOSAL(
        HttpStatus.CONFLICT,
        "REQ-07-017",
        "하나의 수업 교환 제안에 여러 반이 포함될 수 없습니다."
    ),
    REQUEST_LESSONS_NOT_FOUND(
            HttpStatus.CONFLICT,
            "REQ-07-018",
            "수업 교환 요청에 해당하는 기존 수업을 찾을 수 없습니다."
    ),
    INVALID_PROPOSAL_STATUS(
            HttpStatus.CONFLICT,
            "REQ-07-019",
            "현재 상태의 제안은 처리할 수 없습니다."
    ),
    PROPOSAL_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "REQ-07-020",
            "수업 교환 제안을 찾을 수 없습니다."
    ),
    REQUEST_NOT_ACCEPTABLE(
            HttpStatus.CONFLICT,
            "REQ-07-021",
            "현재 상태의 요청에서는 제안을 수락할 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
