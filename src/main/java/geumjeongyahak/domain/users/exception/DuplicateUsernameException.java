package geumjeongyahak.domain.users.exception;

import geumjeongyahak.common.exception.DuplicateResourceException;

/**
 * 중복된 사용자명 예외
 */
public class DuplicateUsernameException extends DuplicateResourceException {

    public DuplicateUsernameException(String username) {
        super(UserErrorCode.DUPLICATE_USERNAME, "이미 존재하는 사용자 아이디입니다: " + username);
    }
}
