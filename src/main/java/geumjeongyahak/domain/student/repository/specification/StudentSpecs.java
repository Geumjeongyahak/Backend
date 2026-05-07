package geumjeongyahak.domain.student.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.enums.StudentStatus;

public class StudentSpecs {

    public static Specification<Student> containsName(String name) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(name)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(root.get("name"), "%" + name + "%");
        };
    }

    public static Specification<Student> hasStatus(StudentStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Student> hasClassroomId(Long classroomId) {
        return (root, query, criteriaBuilder) -> {
            if (classroomId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("classroom").get("id"), classroomId);
        };
    }
}
