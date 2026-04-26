package geumjeongyahak.domain.request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalType;
import geumjeongyahak.domain.request.enums.LessonExchangeScope;
import geumjeongyahak.domain.request.exception.LessonExchangeProposal.InvalidProposalStatusException;
import geumjeongyahak.domain.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "lesson_exchange_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonExchangeProposal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private LessonExchangeRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_by", nullable = false)
    private User proposedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_type", nullable = false, length = 20)
    private LessonExchangeProposalType proposalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_scope", length = 20)
    private LessonExchangeScope proposalScope;

    private LocalDate lessonDate;

    @Column(name = "start_period")
    private Integer startPeriod;

    @Column(name = "end_period")
    private Integer endPeriod;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonExchangeProposalStatus status;

    private LocalDateTime acceptedAt;

    private LocalDateTime withdrawnAt;

    private LocalDateTime closedAt;

    public LessonExchangeProposal(
        LessonExchangeRequest request,
        User proposedBy,
        LessonExchangeProposalType proposalType,
        LessonExchangeScope proposalScope,
        LocalDate lessonDate,
        Integer startPeriod,
        Integer endPeriod,
        String content
    ) {
        this.request = request;
        this.proposedBy = proposedBy;
        this.proposalType = proposalType;
        this.proposalScope = proposalScope;
        this.lessonDate = lessonDate;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.content = content;
        this.status = LessonExchangeProposalStatus.ACTIVE;
    }

    public void accept() {
        validateActive();
        this.status = LessonExchangeProposalStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    public void withdraw() {
        validateActive();
        this.status = LessonExchangeProposalStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }

    public void close() {
        validateActive();
        this.status = LessonExchangeProposalStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    private void validateActive() {
        if (this.status != LessonExchangeProposalStatus.ACTIVE) {
            throw new InvalidProposalStatusException();
        }
    }
}
