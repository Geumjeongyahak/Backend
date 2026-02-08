package sonmoeum.common.advice;

import java.net.URI;
import java.util.stream.Collectors;

import org.springframework.security.authentication.BadCredentialsException;
import sonmoeum.common.error.BusinessException;
import sonmoeum.common.error.ErrorCode;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdvice {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), e.getMessage());
        problemDetail.setTitle(errorCode.getCode());
        problemDetail.setType(URI.create("/errors/" + errorCode.getCode()));
        return ResponseEntity.status(errorCode.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = bindingResult.getAllErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        log.warn("Validation Error: {}", errorMessage);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ErrorCode.INVALID_INPUT_VALUE.getStatus(), errorMessage);
        problemDetail.setTitle(ErrorCode.INVALID_INPUT_VALUE.getCode());
        problemDetail.setType(URI.create("/errors/validation"));
        problemDetail.setProperty("errors", bindingResult.getFieldErrors().stream()
            .collect(Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage(),
                (msg1, msg2) -> msg1 + ", " + msg2
            )));

        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus()).body(problemDetail);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ErrorCode.ACCESS_DENIED.getStatus(), e.getMessage());
        problemDetail.setTitle(ErrorCode.ACCESS_DENIED.getCode());
        problemDetail.setType(URI.create("/errors/access-denied"));
        
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("NoResourceFoundException: {}", e.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ErrorCode.NOT_FOUND.getStatus(), ErrorCode.NOT_FOUND.getMessage());
        problemDetail.setTitle(ErrorCode.NOT_FOUND.getCode());
        problemDetail.setType(URI.create("/errors/not-found"));

        return ResponseEntity.status(ErrorCode.NOT_FOUND.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("BadCredentialsException: {}", e.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            ErrorCode.LOGIN_FAILED.getStatus(),
            ErrorCode.LOGIN_FAILED.getMessage()
        );
        problemDetail.setTitle(ErrorCode.LOGIN_FAILED.getCode());
        problemDetail.setType(URI.create("/errors/" + ErrorCode.LOGIN_FAILED.getCode()));

        return ResponseEntity.status(ErrorCode.LOGIN_FAILED.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ErrorCode.INTERNAL_SERVER_ERROR.getStatus(), "서버 내부 오류가 발생했습니다.");
        problemDetail.setTitle(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        problemDetail.setType(URI.create("/errors/internal-server-error"));

        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).body(problemDetail);
    }
}
