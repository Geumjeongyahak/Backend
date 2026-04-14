package geumjeongyahak.domain.classroom.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;

public class ClassroomSpecs {

    public static Specification<Classroom> containsName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<Classroom> hasType(ClassroomType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Classroom> withoutDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }
}
