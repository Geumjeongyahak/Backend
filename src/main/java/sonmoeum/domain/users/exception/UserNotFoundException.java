package sonmoeum.domain.users.exception;

import sonmoeum.common.exception.ErrorCode;
import sonmoeum.common.exception.ResourceNotFoundException;

/**
 * 사용자를 찾을 수 없는 예외
 */
public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, userId);
    }

    public UserNotFoundException(String username) {
        super(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다. Username: " + username);
    }
}
