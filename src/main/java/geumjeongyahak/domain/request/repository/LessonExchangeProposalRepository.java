package geumjeongyahak.domain.request.repository;

import geumjeongyahak.domain.request.entity.LessonExchangeProposal;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonExchangeProposalRepository
    extends JpaRepository<LessonExchangeProposal, Long> {

    boolean existsByRequest_IdAndProposedBy_IdAndStatus(
        Long requestId,
        Long proposedById,
        LessonExchangeProposalStatus status
    );

    @Query("""
        select count(p) > 0
        from LessonExchangeProposal p
        where p.proposedBy.id = :proposedById
          and p.lessonDate = :lessonDate
          and p.status in :proposalStatuses
          and p.request.status in :requestStatuses
        """)
    boolean existsBlockingProposalByProposedByAndLessonDate(
        @Param("proposedById") Long proposedById,
        @Param("lessonDate") LocalDate lessonDate,
        @Param("proposalStatuses") Collection<LessonExchangeProposalStatus> proposalStatuses,
        @Param("requestStatuses") Collection<LessonExchangeRequestStatus> requestStatuses
    );

    Optional<LessonExchangeProposal> findByIdAndRequest_Id(Long proposalId, Long requestId);

    List<LessonExchangeProposal> findAllByRequest_IdAndStatusNotOrderByCreatedAtDesc(
        Long requestId,
        LessonExchangeProposalStatus status
    );

    long countByStatus(LessonExchangeProposalStatus status);

    long countByRequest_Id(Long requestId);

    long countByRequest_IdAndStatus(Long requestId, LessonExchangeProposalStatus status);
}
