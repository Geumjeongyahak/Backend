package sonmoeum.domain.classroom.repository;

import sonmoeum.domain.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    boolean existsByName(String name);
}
