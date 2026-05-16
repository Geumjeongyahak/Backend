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
@Table(name = "absence_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbsenceRequest extends BaseEntity {

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
    private String reason;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    private LocalDateTime approvalAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_by")
    private User approvalBy;

    @Column(columnDefinition = "TEXT")
    private String note;

    public AbsenceRequest(Lesson lesson, User requestedBy, String title, String reason) {
        this.lesson = lesson;
        this.requestedBy = requestedBy;
        this.title = title;
        this.reason = reason;
        this.expiresAt = lesson.getDate().atStartOfDay();
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

    public void cancel() {
        this.status = RequestStatus.CANCELLED;
    }

    public void expire() {
        this.status = RequestStatus.EXPIRED;
    }
}
