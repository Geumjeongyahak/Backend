package sonmoeum.domain.subject.exception;

import sonmoeum.common.exception.ResourceNotFoundException;

public class SubjectNotFoundException extends ResourceNotFoundException {

    public SubjectNotFoundException(Long subjectId) {
        super(SubjectErrorCode.SUBJECT_NOT_FOUND, subjectId);
    }
}
