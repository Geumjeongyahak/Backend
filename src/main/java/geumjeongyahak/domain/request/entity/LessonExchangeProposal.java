package geumjeongyahak.domain.request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalType;
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

    private LocalDate lessonDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 실제 수업 교사가 변경된 이후에도 제안 화면에는 생성/수정 당시의 반 이름을 유지하기 위한 snapshot 값
    @Column(name = "classroom_name_snapshot")
    private String classroomNameSnapshot;

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
        LocalDate lessonDate,
        String content,
        String classroomNameSnapshot
    ) {
        this.request = request;
        this.proposedBy = proposedBy;
        this.proposalType = proposalType;
        this.lessonDate = lessonDate;
        this.content = content;
        this.classroomNameSnapshot = classroomNameSnapshot;
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

    public void update(
        LessonExchangeProposalType proposalType,
        LocalDate lessonDate,
        String content,
        String classroomNameSnapshot
    ) {
        this.proposalType = proposalType;
        this.lessonDate = lessonDate;
        this.content = content;
        this.classroomNameSnapshot = classroomNameSnapshot;
    }

    private void validateActive() {
        if (this.status != LessonExchangeProposalStatus.ACTIVE) {
            throw new InvalidProposalStatusException();
        }
    }
}
