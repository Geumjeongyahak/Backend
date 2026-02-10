package sonmoeum.domain.student.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.student.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByNameAndPhoneNumber(String name, String phoneNumber);
}
