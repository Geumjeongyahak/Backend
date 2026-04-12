package sonmoeum.common.advice;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;
import sonmoeum.common.exception.CommonErrorCode;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.DuplicateResourceException;
import sonmoeum.common.exception.ResourceNotFoundException;
import sonmoeum.domain.auth.exception.AuthErrorCode;

/**
 * 전역 예외 처리 핸들러
 * - 모든 예외를 RFC 7807 ProblemDetail 형식으로 변환
 * - 일관된 에러 응답 제공
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============ 비즈니스 예외 처리 ============

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException 발생 - Code: {}, Message: {}", ex.getCode(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problemDetail.setTitle(ex.getCode());
        problemDetail.setProperty("code", ex.getCode());

        return ResponseEntity.status(ex.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("ResourceNotFoundException 발생 - Code: {}, Message: {}", ex.getCode(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problemDetail.setTitle(ex.getCode());
        problemDetail.setProperty("code", ex.getCode());

        return ResponseEntity.status(ex.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("DuplicateResourceException 발생 - Code: {}, Message: {}", ex.getCode(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problemDetail.setTitle(ex.getCode());
        problemDetail.setProperty("code", ex.getCode());

        return ResponseEntity.status(ex.getStatus()).body(problemDetail);
    }

    // ============ Spring Security 예외 처리 ============

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Spring Security AuthenticationException 발생 - {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "인증에 실패했습니다."
        );
        problemDetail.setTitle(AuthErrorCode.AUTHENTICATION_FAILED.getCode());
        problemDetail.setProperty("code", AuthErrorCode.AUTHENTICATION_FAILED.getCode());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("BadCredentialsException 발생 - {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                AuthErrorCode.INVALID_CREDENTIALS.getMessage()
        );
        problemDetail.setTitle(AuthErrorCode.INVALID_CREDENTIALS.getCode());
        problemDetail.setProperty("code", AuthErrorCode.INVALID_CREDENTIALS.getCode());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.warn("UsernameNotFoundException 발생 - {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                AuthErrorCode.USER_NOT_FOUND_FOR_AUTH.getMessage()
        );
        problemDetail.setTitle(AuthErrorCode.USER_NOT_FOUND_FOR_AUTH.getCode());
        problemDetail.setProperty("code", AuthErrorCode.USER_NOT_FOUND_FOR_AUTH.getCode());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("AccessDeniedException 발생 - {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                AuthErrorCode.ACCESS_DENIED.getMessage()
        );
        problemDetail.setTitle(AuthErrorCode.ACCESS_DENIED.getCode());
        problemDetail.setProperty("code", AuthErrorCode.ACCESS_DENIED.getCode());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    // ============ Validation 예외 처리 ============

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation 실패 - {}", errorMessages);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                CommonErrorCode.VALIDATION_ERROR.getMessage()
        );
        problemDetail.setTitle(CommonErrorCode.VALIDATION_ERROR.getCode());
        problemDetail.setProperty("code", CommonErrorCode.VALIDATION_ERROR.getCode());
        problemDetail.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex
    ) {
        log.warn("HttpRequestMethodNotSupportedException 발생 - method: {}, supported: {}",
                ex.getMethod(), ex.getSupportedHttpMethods());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                CommonErrorCode.METHOD_NOT_ALLOWED.getMessage()
        );
        problemDetail.setTitle(CommonErrorCode.METHOD_NOT_ALLOWED.getCode());
        problemDetail.setProperty("code", CommonErrorCode.METHOD_NOT_ALLOWED.getCode());
        problemDetail.setProperty("method", ex.getMethod());
        if (ex.getSupportedMethods() != null) {
            problemDetail.setProperty("supportedMethods", ex.getSupportedMethods());
        }

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problemDetail);
    }

    // ============ 데이터베이스 예외 처리 ============

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolationException 발생 - {}", ex.getMessage());

        // 중복 키 에러인 경우
        if (ex.getMessage() != null && ex.getMessage().contains("Unique")) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    CommonErrorCode.DUPLICATE_RESOURCE.getMessage()
            );
            problemDetail.setTitle(CommonErrorCode.DUPLICATE_RESOURCE.getCode());
            problemDetail.setProperty("code", CommonErrorCode.DUPLICATE_RESOURCE.getCode());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
        }

        // 기타 데이터 무결성 위반
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "데이터 무결성 제약 조건 위반입니다."
        );
        problemDetail.setTitle("DATA_INTEGRITY_VIOLATION");
        problemDetail.setProperty("code", "DATA_INTEGRITY_VIOLATION");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // ============ 기타 예외 처리 ============

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("NoResourceFoundException 발생 - {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                CommonErrorCode.RESOURCE_NOT_FOUND.getMessage()
        );
        problemDetail.setTitle(CommonErrorCode.RESOURCE_NOT_FOUND.getCode());
        problemDetail.setProperty("code", CommonErrorCode.RESOURCE_NOT_FOUND.getCode());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException 발생 - {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle(CommonErrorCode.INVALID_INPUT.getCode());
        problemDetail.setProperty("code", CommonErrorCode.INVALID_INPUT.getCode());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception ex) {
        log.error("예상치 못한 오류 발생", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );
        problemDetail.setTitle(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
        problemDetail.setProperty("code", CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    // ============ 내부 클래스 ============

    private record FieldError(String field, String message) {}
}
