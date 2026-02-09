package sonmoeum.domain.users.exception;

import org.springframework.http.HttpStatus;
import sonmoeum.common.exception.BusinessException;

public class DuplicateEmailException extends BusinessException {
    private static final HttpStatus STATUS = HttpStatus.CONFLICT;
    private static final String CODE = "DUPLICATE_EMAIL";

    public DuplicateEmailException(String email) {
        super("이미 존재하는 이메일입니다: " + email, STATUS, CODE);
    }
}
