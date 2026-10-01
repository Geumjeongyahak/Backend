package geumjeongyahak.domain.request.repository;

import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
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

    boolean existsByRequestedBy_IdAndStatusIn(
        Long requesterId,
        Collection<LessonExchangeRequestStatus> statuses
    );

    // 제안을 먼저 닫은 뒤 부른다 (LessonExchangeProposalRepository.closeActiveProposalsOfExpiredRequests)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update LessonExchangeRequest r
        set r.status = geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus.EXPIRED,
            r.version = r.version + 1,
            r.updatedAt = :now
        where r.status in (
                geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus.PENDING,
                geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus.APPROVED
            )
            and r.expiresAt <= :now
        """)
    int expireActiveRequests(@Param("now") LocalDateTime now);

    long countByStatus(LessonExchangeRequestStatus status);

    List<LessonExchangeRequest> findTop10ByStatusOrderByCreatedAtAsc(
        LessonExchangeRequestStatus status
    );
}
