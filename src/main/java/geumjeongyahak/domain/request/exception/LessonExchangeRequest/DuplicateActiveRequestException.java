package geumjeongyahak.domain.request.exception.LessonExchangeRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class DuplicateActiveRequestException extends BusinessException {

    public DuplicateActiveRequestException() {
        super(RequestErrorCode.DUPLICATE_ACTIVE_REQUEST);
    }
}
