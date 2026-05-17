package geumjeongyahak.domain.request.repository;

import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface LessonExchangeRequestRepository extends JpaRepository<LessonExchangeRequest, Long>, JpaSpecificationExecutor<LessonExchangeRequest> {

    Page<LessonExchangeRequest> findAllByStatusNot(
        LessonExchangeRequestStatus status,
        Pageable pageable
    );

    Page<LessonExchangeRequest> findAllByRequestedBy_IdAndStatusNot(
        Long requestedById,
        LessonExchangeRequestStatus status,
        Pageable pageable
    );

    Page<LessonExchangeRequest> findAllByStatus(
        LessonExchangeRequestStatus status,
        Pageable pageable
    );

    Page<LessonExchangeRequest> findAllByStatusAndRequestedBy_Id(
        LessonExchangeRequestStatus status,
        Long requestedById,
        Pageable pageable
    );

    List<LessonExchangeRequest> findAllByRequestedBy_IdAndDailySchedule_IdAndStatusIn(
        Long requesterId,
        Long dailyScheduleId,
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

    long countByStatus(LessonExchangeRequestStatus status);

    List<LessonExchangeRequest> findTop10ByStatusOrderByCreatedAtAsc(
        LessonExchangeRequestStatus status
    );
}
