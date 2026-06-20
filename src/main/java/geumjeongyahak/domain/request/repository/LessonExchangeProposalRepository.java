package geumjeongyahak.domain.request.repository;

import geumjeongyahak.domain.request.entity.LessonExchangeProposal;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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

    long countByRequest_Id(Long requestId);

    long countByRequest_IdAndStatus(Long requestId, LessonExchangeProposalStatus status);
}
