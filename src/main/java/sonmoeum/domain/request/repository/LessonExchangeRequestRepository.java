package sonmoeum.domain.request.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.request.entity.LessonExchangeRequest;
import sonmoeum.domain.request.enums.RequestStatus;

public interface LessonExchangeRequestRepository extends JpaRepository<LessonExchangeRequest, Long> {

    List<LessonExchangeRequest> findAllByOrderByCreatedAtDesc();

    List<LessonExchangeRequest> findAllByRequestedBy_IdOrderByCreatedAtDesc(Long requestedById);

    List<LessonExchangeRequest> findAllByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<LessonExchangeRequest> findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
        RequestStatus status,
        Long requestedById
    );
}
