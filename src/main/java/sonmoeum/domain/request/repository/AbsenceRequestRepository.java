package sonmoeum.domain.request.repository;

import sonmoeum.domain.request.entity.AbsenceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {
}
