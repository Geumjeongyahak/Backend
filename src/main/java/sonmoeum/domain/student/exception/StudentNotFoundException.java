package sonmoeum.domain.student.exception;

import sonmoeum.common.exception.ErrorCode;
import sonmoeum.common.exception.ResourceNotFoundException;

public class StudentNotFoundException extends ResourceNotFoundException {

    public StudentNotFoundException(Long studentId) {
        super(ErrorCode.STUDENT_NOT_FOUND, studentId);
    }
}
