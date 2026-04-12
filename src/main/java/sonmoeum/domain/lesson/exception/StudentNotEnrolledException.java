package sonmoeum.domain.lesson.exception;

import sonmoeum.common.exception.ResourceNotFoundException;

public class StudentNotEnrolledException extends ResourceNotFoundException {

    public StudentNotEnrolledException(Long studentId) {
        super(LessonErrorCode.STUDENT_NOT_ENROLLED, studentId);
    }
}
