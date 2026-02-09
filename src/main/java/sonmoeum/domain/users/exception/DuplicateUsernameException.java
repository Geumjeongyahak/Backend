package sonmoeum.domain.users.exception;

import org.springframework.http.HttpStatus;
import sonmoeum.common.exception.BusinessException;

public class DuplicateUsernameException extends BusinessException {
    private static final HttpStatus STATUS = HttpStatus.CONFLICT;
    private static final String CODE = "DUPLICATE_USERNAME";

    public DuplicateUsernameException(String username) {
        super("이미 존재하는 사용자 아이디입니다: " + username, STATUS, CODE);
    }
}
