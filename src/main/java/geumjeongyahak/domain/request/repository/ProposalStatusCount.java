package geumjeongyahak.domain.request.repository;

import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;

public record ProposalStatusCount(Long requestId, LessonExchangeProposalStatus status, Long count) {
}
