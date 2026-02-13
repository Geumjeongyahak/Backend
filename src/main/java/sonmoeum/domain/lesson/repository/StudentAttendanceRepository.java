package sonmoeum.domain.lesson.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.lesson.entity.StudentAttendance;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long> {

    @EntityGraph(attributePaths = {"student"})
    List<StudentAttendance> findAllByLessonId(Long lessonId);
}
