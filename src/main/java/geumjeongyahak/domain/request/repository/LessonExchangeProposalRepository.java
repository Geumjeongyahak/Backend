package geumjeongyahak.domain.request.repository;

import geumjeongyahak.domain.request.entity.LessonExchangeProposal;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonExchangeProposalRepository
    extends JpaRepository<LessonExchangeProposal, Long> {

    boolean existsByRequest_IdAndProposedBy_IdAndStatus(
        Long requestId,
        Long proposedById,
        LessonExchangeProposalStatus status
    );

    boolean existsByProposedBy_IdAndLessonDateAndStatusIn(
        Long proposedById,
        LocalDate lessonDate,
        Collection<LessonExchangeProposalStatus> statuses
    );

    boolean existsByProposedBy_IdAndStatus(
        Long proposedById,
        LessonExchangeProposalStatus status
    );

    Optional<LessonExchangeProposal> findByIdAndRequest_Id(Long proposalId, Long requestId);

    List<LessonExchangeProposal> findAllByRequest_IdAndStatusNotOrderByCreatedAtDesc(
        Long requestId,
        LessonExchangeProposalStatus status
    );

    long countByStatus(LessonExchangeProposalStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update LessonExchangeProposal p
        set p.status = geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus.CLOSED,
            p.closedAt = :now,
            p.updatedAt = :now
        where p.status = geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus.ACTIVE
            and p.request.id in (
                select r.id from LessonExchangeRequest r
                where r.status in (
                        geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus.PENDING,
                        geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus.APPROVED
                    )
                    and r.expiresAt <= :now
            )
        """)
    int closeActiveProposalsOfExpiredRequests(@Param("now") LocalDateTime now);

    @Query("""
        select new geumjeongyahak.domain.request.repository.ProposalStatusCount(p.request.id, p.status, count(p))
        from LessonExchangeProposal p
        where p.request.id in :requestIds
        group by p.request.id, p.status
        """)
    List<ProposalStatusCount> countByRequestIdsGroupByStatus(@Param("requestIds") Collection<Long> requestIds);
}
