package sonmoeum.domain.request.repository;

import sonmoeum.domain.request.entity.SubjectExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectExchangeRequestRepository extends JpaRepository<SubjectExchangeRequest, Long> {
}
