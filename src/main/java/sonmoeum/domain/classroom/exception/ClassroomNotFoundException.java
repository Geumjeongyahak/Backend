package sonmoeum.domain.classroom.exception;

import sonmoeum.common.exception.ErrorCode;
import sonmoeum.common.exception.ResourceNotFoundException;

public class ClassroomNotFoundException extends ResourceNotFoundException {

    public ClassroomNotFoundException(Long classId) {
        super(ErrorCode.CLASSROOM_NOT_FOUND, classId);
    }
}
