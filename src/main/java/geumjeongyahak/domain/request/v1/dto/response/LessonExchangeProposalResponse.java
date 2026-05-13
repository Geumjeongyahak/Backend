package geumjeongyahak.domain.request.v1.dto.response;

import geumjeongyahak.domain.request.entity.LessonExchangeProposal;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LessonExchangeProposalResponse(

    @Schema(description = "제안 ID", example = "1")
    Long id,

    @Schema(description = "요청 ID", example = "3")
    Long requestId,

    @Schema(description = "반 이름", example = "장미반")
    String classroomName,

    @Schema(description = "제안자 ID", example = "3")
    Long proposedById,

    @Schema(description = "제안자 이름", example = "김철수")
    String proposedByName,

    @Schema(description = "제안 유형", example = "EXCHANGE")
    LessonExchangeProposalType proposalType,

    @Schema(description = "제안 수업 날짜", example = "2026-06-17")
    LocalDate lessonDate,

    @Schema(description = "제안 내용")
    String content,

    @Schema(description = "제안 상태", example = "ACTIVE")
    LessonExchangeProposalStatus status,

    @Schema(description = "제안 수락 시각")
    LocalDateTime acceptedAt,

    @Schema(description = "제안 철회 시각")
    LocalDateTime withdrawnAt,

    @Schema(description = "제안 종료 시각")
    LocalDateTime closedAt,

    @Schema(description = "생성 시각")
    LocalDateTime createdAt
) {
    public static LessonExchangeProposalResponse from(LessonExchangeProposal proposal) {
        return new LessonExchangeProposalResponse(
            proposal.getId(),
            proposal.getRequest().getId(),
            proposal.getClassroomNameSnapshot(),
            proposal.getProposedBy().getId(),
            proposal.getProposedBy().getName(),
            proposal.getProposalType(),
            proposal.getLessonDate(),
            proposal.getContent(),
            proposal.getStatus(),
            proposal.getAcceptedAt(),
            proposal.getWithdrawnAt(),
            proposal.getClosedAt(),
            proposal.getCreatedAt()
        );
    }
}
