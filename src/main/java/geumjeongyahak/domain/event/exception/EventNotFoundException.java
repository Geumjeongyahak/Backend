package geumjeongyahak.domain.event.exception;

import geumjeongyahak.common.exception.ResourceNotFoundException;

public class EventNotFoundException extends ResourceNotFoundException {

    public EventNotFoundException(Long id) {
        super(EventErrorCode.EVENT_NOT_FOUND, id);
    }
}
