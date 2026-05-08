package geumjeongyahak.domain.student.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import geumjeongyahak.domain.student.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByIdAndIsDeletedFalse(Long id);
    boolean existsByNameAndPhoneNumberAndIsDeletedFalse(String name, String phoneNumber);
    boolean existsByNameAndPhoneNumberAndIdNotAndIsDeletedFalse(String newName, String newPhone, Long studentId);
}
