package geumjeongyahak.domain.request.repository;

import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        select r
        from LessonExchangeRequest r
        where r.requestedBy.id = :requesterId
          and r.dailySchedule.id = :dailyScheduleId
          and r.status in :activeStatuses
          and r.cancelledAt is null
        """)
    List<LessonExchangeRequest> findBlockingActiveRequests(
        @Param("requesterId") Long requesterId,
        @Param("dailyScheduleId") Long dailyScheduleId,
        @Param("activeStatuses") Collection<LessonExchangeRequestStatus> activeStatuses
    );

    @Query("""
        select count(r) > 0
        from LessonExchangeRequest r
        where r.requestedBy.id = :requesterId
          and r.lessonDate = :lessonDate
          and r.status in :statuses
          and r.cancelledAt is null
        """)
    boolean existsBlockingActiveRequestByRequesterAndLessonDate(
        @Param("requesterId") Long requesterId,
        @Param("lessonDate") LocalDate lessonDate,
        @Param("statuses") Collection<LessonExchangeRequestStatus> statuses
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
