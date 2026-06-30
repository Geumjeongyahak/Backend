package geumjeongyahak.domain.teacher_application.exception;

import geumjeongyahak.common.exception.ResourceNotFoundException;

public class TeacherApplicationNotFoundException extends ResourceNotFoundException {

    public TeacherApplicationNotFoundException(Long id) {
        super(TeacherApplicationErrorCode.TEACHER_APPLICATION_NOT_FOUND, id);
    }
}
