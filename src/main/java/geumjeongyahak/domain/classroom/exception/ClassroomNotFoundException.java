package geumjeongyahak.domain.classroom.exception;

import geumjeongyahak.common.exception.ResourceNotFoundException;

public class ClassroomNotFoundException extends ResourceNotFoundException {

    public ClassroomNotFoundException(Long classId) {
        super(ClassroomErrorCode.CLASSROOM_NOT_FOUND, classId);
    }
}
