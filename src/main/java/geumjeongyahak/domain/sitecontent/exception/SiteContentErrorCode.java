package geumjeongyahak.domain.sitecontent.exception;

import geumjeongyahak.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SiteContentErrorCode implements ErrorCode {
    SITE_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-13-000", "사이트 콘텐츠를 찾을 수 없습니다."),
    DEPARTMENT_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-13-001", "기관 부서 콘텐츠를 찾을 수 없습니다."),
    CLASS_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-13-002", "기관 반 콘텐츠를 찾을 수 없습니다."),
    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-13-003", "연혁 콘텐츠를 찾을 수 없습니다."),
    PRINCIPAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "BIZ-13-001", "교장 콘텐츠는 하나만 등록할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
