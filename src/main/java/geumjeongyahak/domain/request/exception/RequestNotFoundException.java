package geumjeongyahak.domain.request.exception;

import geumjeongyahak.common.exception.ResourceNotFoundException;

public class RequestNotFoundException extends ResourceNotFoundException {

    public RequestNotFoundException(Long id) {
        super(RequestErrorCode.REQUEST_NOT_FOUND, id);
    }
}
