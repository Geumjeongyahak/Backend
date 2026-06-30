package geumjeongyahak.domain.auth.exception;

import geumjeongyahak.common.exception.AuthenticationException;

public class OAuthProcessingException extends AuthenticationException {

    public OAuthProcessingException(String message) {
        super(AuthErrorCode.OAUTH_PROCESSING_FAILED, message);
    }

    public OAuthProcessingException(String message, Throwable cause) {
        super(AuthErrorCode.OAUTH_PROCESSING_FAILED, cause);
    }
}
