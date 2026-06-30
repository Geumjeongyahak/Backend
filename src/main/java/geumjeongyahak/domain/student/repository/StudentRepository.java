package geumjeongyahak.domain.student.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.enums.StudentStatus;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByIdAndIsDeletedFalse(Long id);
    @Query("""
            SELECT DISTINCT s
            FROM Student s
            JOIN s.studentClassrooms sc
            WHERE sc.classroom.id = :classroomId
              AND s.status = :status
              AND s.isDeleted = false
              AND sc.isDeleted = false
            """)
    List<Student> findAllByClassroomIdAndStatusAndIsDeletedFalse(
            @Param("classroomId") Long classroomId,
            @Param("status") StudentStatus status
    );
    boolean existsByNameAndPhoneNumberAndIsDeletedFalse(String name, String phoneNumber);
    boolean existsByNameAndPhoneNumberAndIdNotAndIsDeletedFalse(String newName, String newPhone, Long studentId);
}
