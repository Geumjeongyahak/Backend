package sonmoeum.domain.student.exception;

import sonmoeum.common.exception.ResourceNotFoundException;

public class StudentNotFoundException extends ResourceNotFoundException {

    public StudentNotFoundException(Long studentId) {
        super(StudentErrorCode.STUDENT_NOT_FOUND, studentId);
    }
}
