package geumjeongyahak.domain.student.exception;

import geumjeongyahak.common.exception.ResourceNotFoundException;

public class StudentNotFoundException extends ResourceNotFoundException {

    public StudentNotFoundException(Long studentId) {
        super(StudentErrorCode.STUDENT_NOT_FOUND, studentId);
    }
}
