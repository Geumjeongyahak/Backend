package geumjeongyahak.domain.request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_lesson_id", nullable = false)
    private Lesson proposedLesson;

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
        Lesson proposedLesson,
        String content
    ) {
        this.request = request;
        this.proposedBy = proposedBy;
        this.proposedLesson = proposedLesson;
        this.content = content;
        this.status = LessonExchangeProposalStatus.ACTIVE;
    }

    public void accept() {
        this.status = LessonExchangeProposalStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    public void withdraw() {
        this.status = LessonExchangeProposalStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }

    public void close() {
        this.status = LessonExchangeProposalStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }
}
