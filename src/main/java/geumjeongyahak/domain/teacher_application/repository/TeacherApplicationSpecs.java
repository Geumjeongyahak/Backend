package geumjeongyahak.domain.teacher_application.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import geumjeongyahak.domain.teacher_application.entity.TeacherApplication;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;

public final class TeacherApplicationSpecs {

    private TeacherApplicationSpecs() {
    }

    public static Specification<TeacherApplication> hasStatus(TeacherApplicationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<TeacherApplication> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("applicantName")), pattern),
                cb.like(cb.lower(root.get("applicantPhoneNumber")), pattern),
                cb.like(cb.lower(root.get("applicantEmail")), pattern),
                cb.like(cb.lower(root.get("preferredSubject").get("name")), pattern)
            );
        };
    }
}
