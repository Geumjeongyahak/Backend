package geumjeongyahak.domain.lesson.exception;

import geumjeongyahak.common.exception.ResourceNotFoundException;

public class StudentNotEnrolledException extends ResourceNotFoundException {

    public StudentNotEnrolledException(Long studentId) {
        super(LessonErrorCode.STUDENT_NOT_ENROLLED, studentId);
    }
}
