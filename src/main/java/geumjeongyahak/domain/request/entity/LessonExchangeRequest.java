package geumjeongyahak.domain.request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeScope;
import geumjeongyahak.domain.users.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "lesson_exchange_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonExchangeRequest extends BaseEntity {

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
    private LessonExchangeRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonExchangeScope scope;

    @Column(name = "start_period")
    private Integer startPeriod;

    @Column(name = "end_period")
    private Integer endPeriod;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionNote;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonExchangeProposal> proposals = new ArrayList<>();

    public LessonExchangeRequest(
        Lesson lesson,
        User requestedBy,
        String title,
        String content,
        LessonExchangeScope scope,
        Integer startPeriod,
        Integer endPeriod,
        LocalDateTime expiresAt
    ) {
        validateScope(scope, startPeriod, endPeriod);
        this.lesson = lesson;
        this.requestedBy = requestedBy;
        this.title = title;
        this.content = content;
        this.status = LessonExchangeRequestStatus.PENDING;
        this.scope = scope;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.expiresAt = expiresAt;
    }

    public void approve(User approver) {
        this.status = LessonExchangeRequestStatus.APPROVED;
        this.processedBy = approver;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(User approver, String rejectionNote) {
        this.status = LessonExchangeRequestStatus.REJECTED;
        this.processedBy = approver;
        this.processedAt = LocalDateTime.now();
        this.rejectionNote = rejectionNote;
    }

    public void complete() {
        this.status = LessonExchangeRequestStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = LessonExchangeRequestStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = LessonExchangeRequestStatus.EXPIRED;
    }

    private static void validateScope(
        LessonExchangeScope scope,
        Integer startPeriod,
        Integer endPeriod
    ) {
        if (scope == LessonExchangeScope.FULL) {
            if (startPeriod != null || endPeriod != null) {
                throw new IllegalArgumentException("전체 교환은 교시 범위를 가질 수 없습니다.");
            }
            return;
        }

        if (startPeriod == null || endPeriod == null) {
            throw new IllegalArgumentException("부분 교환은 시작/종료 교시가 필요합니다.");
        }

        if (startPeriod < 1 || endPeriod > 3 || startPeriod > endPeriod) {
            throw new IllegalArgumentException("유효하지 않은 교시 범위입니다.");
        }
    }
}

