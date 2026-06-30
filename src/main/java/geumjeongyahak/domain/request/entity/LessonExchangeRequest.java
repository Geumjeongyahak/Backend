package geumjeongyahak.domain.request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
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
    @JoinColumn(name = "daily_schedule_id", nullable = false)
    private DailySchedule dailySchedule;

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
        DailySchedule dailySchedule,
        User requestedBy,
        String title,
        String classroomNameSnapshot,
        String content,
        LocalDateTime expiresAt
    ) {
        this.dailySchedule = dailySchedule;
        this.requestedBy = requestedBy;
        this.lessonDate = dailySchedule.getLessonDate();
        this.title = title;
        this.classroomNameSnapshot = classroomNameSnapshot;
        this.content = content;
        this.status = LessonExchangeRequestStatus.PENDING;
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
        DailySchedule dailySchedule,
        String title,
        String classroomNameSnapshot,
        String content,
        LocalDateTime expiresAt
    ) {
        this.dailySchedule = dailySchedule;
        this.lessonDate = dailySchedule.getLessonDate();
        this.title = title;
        this.classroomNameSnapshot = classroomNameSnapshot;
        this.content = content;
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
}

