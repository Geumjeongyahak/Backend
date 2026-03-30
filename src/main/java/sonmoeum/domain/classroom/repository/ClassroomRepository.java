package sonmoeum.domain.classroom.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sonmoeum.domain.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long>, JpaSpecificationExecutor<Classroom> {

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
