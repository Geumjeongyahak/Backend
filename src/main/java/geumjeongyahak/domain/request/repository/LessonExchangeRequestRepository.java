package geumjeongyahak.domain.request.repository;

import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface LessonExchangeRequestRepository extends JpaRepository<LessonExchangeRequest, Long> {

    List<LessonExchangeRequest> findAllByStatusNotOrderByCreatedAtDesc(
        LessonExchangeRequestStatus status
    );

    List<LessonExchangeRequest> findAllByRequestedBy_IdAndStatusNotOrderByCreatedAtDesc(
        Long requestedById,
        LessonExchangeRequestStatus status
    );

    List<LessonExchangeRequest> findAllByStatusOrderByCreatedAtDesc(
        LessonExchangeRequestStatus status
    );

    List<LessonExchangeRequest> findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
        LessonExchangeRequestStatus status,
        Long requestedById
    );

    List<LessonExchangeRequest> findAllByRequestedBy_IdAndLessonDateAndStatusIn(
        Long requesterId,
        LocalDate lessonDate,
        Collection<LessonExchangeRequestStatus> activeStatuses
    );

    boolean existsByRequestedBy_IdAndLessonDateAndStatusIn(
        Long requesterId,
        LocalDate lessonDate,
        Collection<LessonExchangeRequestStatus> statuses
    );

    List<LessonExchangeRequest> findAllByStatusInAndExpiresAtBefore(
        Collection<LessonExchangeRequestStatus> statuses,
        LocalDateTime expiresAt
    );
}
