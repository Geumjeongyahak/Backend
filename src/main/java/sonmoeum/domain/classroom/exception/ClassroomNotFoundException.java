package sonmoeum.domain.classroom.exception;

import sonmoeum.common.exception.ResourceNotFoundException;

public class ClassroomNotFoundException extends ResourceNotFoundException {

    public ClassroomNotFoundException(Long classId) {
        super(ClassroomErrorCode.CLASSROOM_NOT_FOUND, classId);
    }
}
