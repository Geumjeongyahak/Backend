package geumjeongyahak.domain.student.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import geumjeongyahak.domain.student.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    boolean existsByNameAndPhoneNumber(String name, String phoneNumber);
    boolean existsByNameAndPhoneNumberAndIdNot(String newName, String newPhone, Long studentId);
}
