package geumjeongyahak.domain.student.exception;

import geumjeongyahak.common.exception.DuplicateResourceException;

public class DuplicateStudentException extends DuplicateResourceException {
    public DuplicateStudentException(String name, String phoneNumber) {
        super(StudentErrorCode.DUPLICATE_STUDENT, "이미 등록된 학생입니다. 이름: " + name + ", 전화번호: " + phoneNumber);
    }
}
