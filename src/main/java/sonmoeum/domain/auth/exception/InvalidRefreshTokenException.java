package sonmoeum.domain.auth.exception;

import org.springframework.http.HttpStatus;
import sonmoeum.common.exception.BusinessException;

public class InvalidRefreshTokenException extends BusinessException {


    public InvalidRefreshTokenException() {
        super("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
    }
}
