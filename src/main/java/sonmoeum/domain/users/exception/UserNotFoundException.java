package sonmoeum.domain.users.exception;

import org.springframework.http.HttpStatus;
import sonmoeum.common.exception.BusinessException;

public class UserNotFoundException extends BusinessException {
    private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;
    private static final String CODE = "USER_NOT_FOUND";

    public UserNotFoundException(Long userId) {
        super("사용자를 찾을 수 없습니다. ID: " + userId, STATUS, CODE);
    }

    public UserNotFoundException(String username) {
        super("사용자를 찾을 수 없습니다. Username: " + username, STATUS, CODE);
    }
}
