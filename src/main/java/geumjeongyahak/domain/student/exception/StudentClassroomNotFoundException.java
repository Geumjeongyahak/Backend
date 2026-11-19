package geumjeongyahak.domain.student.exception;

import geumjeongyahak.common.exception.ResourceNotFoundException;

public class StudentClassroomNotFoundException extends ResourceNotFoundException {

    public StudentClassroomNotFoundException(Long studentId, Long classroomId) {
        super(
            StudentErrorCode.STUDENT_CLASSROOM_NOT_FOUND,
            String.format(
                "%s (studentId: %d, classroomId: %d)",
                StudentErrorCode.STUDENT_CLASSROOM_NOT_FOUND.getMessage(),
                studentId,
                classroomId
            )
        );
    }
}
