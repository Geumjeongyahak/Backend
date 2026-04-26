package geumjeongyahak.domain.request.repository;

import geumjeongyahak.domain.request.entity.LessonExchangeProposal;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonExchangeProposalRepository
    extends JpaRepository<LessonExchangeProposal, Long> {

    boolean existsByRequest_IdAndProposedBy_IdAndStatus(
        Long requestId,
        Long proposedById,
        LessonExchangeProposalStatus status
    );
}
