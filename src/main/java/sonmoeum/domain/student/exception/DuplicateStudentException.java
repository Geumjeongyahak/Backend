package sonmoeum.domain.student.exception;

import sonmoeum.common.exception.DuplicateResourceException;
import sonmoeum.common.exception.ErrorCode;

public class DuplicateStudentException extends DuplicateResourceException {
    public DuplicateStudentException(String name, String phoneNumber) {
        super(ErrorCode.DUPLICATE_STUDENT, "이미 등록된 학생입니다. 이름: " + name + ", 전화번호: " + phoneNumber);
    }
}
