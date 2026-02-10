package sonmoeum.domain.users.exception;

import sonmoeum.common.exception.DuplicateResourceException;
import sonmoeum.common.exception.ErrorCode;

/**
 * 중복된 이메일 예외
 */
public class DuplicateEmailException extends DuplicateResourceException {

    public DuplicateEmailException(String email) {
        super(ErrorCode.DUPLICATE_EMAIL, "이미 존재하는 이메일입니다: " + email);
    }
}
