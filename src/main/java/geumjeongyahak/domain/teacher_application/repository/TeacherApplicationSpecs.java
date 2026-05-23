package geumjeongyahak.domain.teacher_application.repository;

import org.springframework.data.jpa.domain.Specification;

import geumjeongyahak.domain.teacher_application.entity.TeacherApplication;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;

public final class TeacherApplicationSpecs {

    private TeacherApplicationSpecs() {
    }

    public static Specification<TeacherApplication> hasStatus(TeacherApplicationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<TeacherApplication> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword + "%";
            return cb.or(
                cb.like(root.get("applicantName"), pattern),
                cb.like(root.get("applicantPhoneNumber"), pattern),
                cb.like(root.get("applicantEmail"), pattern),
                cb.like(root.get("preferredSubject").get("name"), pattern)
            );
        };
    }
}
