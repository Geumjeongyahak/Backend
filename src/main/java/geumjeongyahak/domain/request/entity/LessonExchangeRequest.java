package geumjeongyahak.domain.request.entity;

import java.time.LocalDateTime;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.users.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "lesson_exchange_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonExchangeRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    private LocalDateTime approvalAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_by")
    private User approvalBy;

    @Column(columnDefinition = "TEXT")
    private String note;

    public LessonExchangeRequest(Lesson lesson, User requestedBy, String title, String content) {
        this.lesson = lesson;
        this.requestedBy = requestedBy;
        this.title = title;
        this.content = content;
        this.status = RequestStatus.PENDING;
    }

    public void approve(User approver) {
        this.status = RequestStatus.APPROVED;
        this.approvalBy = approver;
        this.approvalAt = LocalDateTime.now();
    }

    public void reject(User approver, String note) {
        this.status = RequestStatus.REJECTED;
        this.approvalBy = approver;
        this.approvalAt = LocalDateTime.now();
        this.note = note;
    }
}

