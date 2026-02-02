package sonmoeum.domain.subject.repository;

import sonmoeum.domain.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
}
