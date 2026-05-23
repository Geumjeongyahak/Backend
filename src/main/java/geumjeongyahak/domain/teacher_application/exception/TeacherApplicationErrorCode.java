package geumjeongyahak.domain.teacher_application.exception;

import org.springframework.http.HttpStatus;

import geumjeongyahak.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeacherApplicationErrorCode implements ErrorCode {
    TEACHER_APPLICATION_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "RES-12-001",
        "교원 신청을 찾을 수 없습니다."
    ),
    TEACHER_APPLICATION_ALREADY_PENDING(
        HttpStatus.CONFLICT,
        "BIZ-12-001",
        "이미 검토 대기 중인 교원 신청이 존재합니다."
    ),
    INVALID_TEACHER_APPLICATION_STATUS(
        HttpStatus.CONFLICT,
        "BIZ-12-002",
        "현재 상태의 교원 신청은 처리할 수 없습니다."
    ),
    TEACHER_APPLICATION_FORBIDDEN(
        HttpStatus.FORBIDDEN,
        "AUTH-12-001",
        "해당 교원 신청에 대한 권한이 없습니다."
    ),
    TEACHER_APPLICATION_APPLICANT_NOT_GUEST(
        HttpStatus.FORBIDDEN,
        "AUTH-12-002",
        "교원 신청자는 GUEST 상태여야 합니다."
    ),
    INVALID_PREFERRED_SUBJECT(
        HttpStatus.BAD_REQUEST,
        "VAL-12-001",
        "교사가 미배정된 활성 과목만 선호 과목으로 신청할 수 있습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
