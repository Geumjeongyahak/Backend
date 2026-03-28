package sonmoeum.domain.request.exception;

import sonmoeum.common.exception.ErrorCode;
import sonmoeum.common.exception.ResourceNotFoundException;

public class RequestNotFoundException extends ResourceNotFoundException {

    public RequestNotFoundException(Long id) {
        super(ErrorCode.REQUEST_NOT_FOUND, id);
    }
}
