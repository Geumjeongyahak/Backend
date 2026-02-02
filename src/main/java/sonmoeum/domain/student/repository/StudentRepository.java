package sonmoeum.domain.student.repository;

import sonmoeum.domain.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
}
