package geumjeongyahak.domain.request.repository;

import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonExchangeRequestRepository extends JpaRepository<LessonExchangeRequest, Long> {

    List<LessonExchangeRequest> findAllByOrderByCreatedAtDesc();

    List<LessonExchangeRequest> findAllByRequestedBy_IdOrderByCreatedAtDesc(Long requestedById);

    List<LessonExchangeRequest> findAllByStatusOrderByCreatedAtDesc(
        LessonExchangeRequestStatus status
    );

    List<LessonExchangeRequest> findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
        LessonExchangeRequestStatus status,
        Long requestedById
    );

    boolean existsByLesson_IdAndStatusIn(
        Long lessonId,
        Collection<LessonExchangeRequestStatus> statuses
    );
}
