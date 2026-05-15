package geumjeongyahak.domain.student.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.enums.StudentStatus;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByIdAndIsDeletedFalse(Long id);
    List<Student> findAllByClassroomIdAndStatusAndIsDeletedFalse(Long classroomId, StudentStatus status);
    boolean existsByNameAndPhoneNumberAndIsDeletedFalse(String name, String phoneNumber);
    boolean existsByNameAndPhoneNumberAndIdNotAndIsDeletedFalse(String newName, String newPhone, Long studentId);
}
