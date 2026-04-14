package geumjeongyahak.domain.student.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.enums.StudentStatus;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByNameAndPhoneNumber(String name, String phoneNumber);
    boolean existsByNameAndPhoneNumberAndIdNot(String newName, String newPhone, Long studentId);
    Page<Student> findAllBy(Pageable pageable);
    Page<Student> findAllByNameContaining(String name, Pageable pageable);
    Page<Student> findAllByStatus(StudentStatus status, Pageable pageable);
    Page<Student> findAllByNameContainingAndStatus(String name, StudentStatus status, Pageable pageable);
}
