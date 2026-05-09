package geumjeongyahak.domain.subject.exception;

import geumjeongyahak.common.exception.BusinessException;

public class SubjectTeacherAssignmentConflictException extends BusinessException {

    public SubjectTeacherAssignmentConflictException(String customMessage) {
        super(SubjectErrorCode.SUBJECT_TEACHER_ASSIGNMENT_CONFLICT, customMessage);
    }
}
