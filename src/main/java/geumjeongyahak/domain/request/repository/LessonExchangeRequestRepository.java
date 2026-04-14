package geumjeongyahak.domain.request.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.RequestStatus;

public interface LessonExchangeRequestRepository extends JpaRepository<LessonExchangeRequest, Long> {

    List<LessonExchangeRequest> findAllByOrderByCreatedAtDesc();

    List<LessonExchangeRequest> findAllByRequestedBy_IdOrderByCreatedAtDesc(Long requestedById);

    List<LessonExchangeRequest> findAllByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<LessonExchangeRequest> findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
        RequestStatus status,
        Long requestedById
    );
}
