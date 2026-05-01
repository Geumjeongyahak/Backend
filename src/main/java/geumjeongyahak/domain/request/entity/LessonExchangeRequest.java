package geumjeongyahak.domain.request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeScope;
import geumjeongyahak.domain.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "lesson_exchange_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonExchangeRequest extends BaseEntity {

    @Column(nullable = false)
    private LocalDate lessonDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(nullable = false)
    private String title;

    // 실제 수업 교사가 변경된 이후에도 요청 화면에는 생성/수정 당시의 반 이름을 유지하기 위한 snapshot 값
    @Column(name = "classroom_name_snapshot", nullable = false)
    private String classroomNameSnapshot;

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
        User requestedBy,
        LocalDate lessonDate,
        String title,
        String classroomNameSnapshot,
        String content,
        LessonExchangeScope scope,
        Integer startPeriod,
        Integer endPeriod,
        LocalDateTime expiresAt
    ) {
        validateScope(scope, startPeriod, endPeriod);
        this.requestedBy = requestedBy;
        this.lessonDate = lessonDate;
        this.title = title;
        this.classroomNameSnapshot = classroomNameSnapshot;
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

    public void update(
        LocalDate lessonDate,
        String title,
        String classroomNameSnapshot,
        String content,
        LessonExchangeScope scope,
        Integer startPeriod,
        Integer endPeriod,
        LocalDateTime expiresAt
    ) {
        validateScope(scope, startPeriod, endPeriod);
        this.lessonDate = lessonDate;
        this.title = title;
        this.classroomNameSnapshot = classroomNameSnapshot;
        this.content = content;
        this.scope = scope;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.expiresAt = expiresAt;
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

