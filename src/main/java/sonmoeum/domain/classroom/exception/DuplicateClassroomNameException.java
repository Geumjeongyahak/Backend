package sonmoeum.domain.classroom.exception;

import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;

public class DuplicateClassroomNameException extends BusinessException {

    public DuplicateClassroomNameException() {
        super(ErrorCode.DUPLICATE_CLASSROOM);
    }
}
