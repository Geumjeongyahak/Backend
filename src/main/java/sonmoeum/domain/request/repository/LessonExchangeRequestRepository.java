package sonmoeum.domain.request.repository;

import sonmoeum.domain.request.entity.LessonExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonExchangeRequestRepository extends JpaRepository<LessonExchangeRequest, Long> {
}
