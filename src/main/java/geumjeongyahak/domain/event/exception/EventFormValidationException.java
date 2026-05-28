package geumjeongyahak.domain.event.exception;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.exception.CommonErrorCode;

public class EventFormValidationException extends BadRequestException {

    public EventFormValidationException(String message) {
        super(CommonErrorCode.VALIDATION_ERROR, message);
    }
}
