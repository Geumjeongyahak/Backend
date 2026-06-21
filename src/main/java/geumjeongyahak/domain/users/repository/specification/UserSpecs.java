package geumjeongyahak.domain.users.repository.specification;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class UserSpecs {
    public static Specification<User> isActive() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<User> hasRole(RoleType role) {
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<User> containsName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<User> isCurrentTeacher(LocalDate today) {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("teacherStartAt")),
            cb.lessThanOrEqualTo(root.get("teacherStartAt"), today),
            cb.or(
                cb.isNull(root.get("teacherEndAt")),
                cb.greaterThanOrEqualTo(root.get("teacherEndAt"), today)
            )
        );
    }
}
