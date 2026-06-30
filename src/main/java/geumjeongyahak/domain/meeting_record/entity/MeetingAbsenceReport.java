package geumjeongyahak.domain.meeting_record.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@Table(name = "meeting_absence_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingAbsenceReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_record_id", nullable = false)
    private MeetingRecord meetingRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String opinion;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    public MeetingAbsenceReport(@NonNull User author, @NonNull String reason, String opinion) {
        this.author = author;
        this.reason = reason;
        this.opinion = opinion;
        this.isDeleted = false;
    }

    void assignMeetingRecord(MeetingRecord meetingRecord) {
        this.meetingRecord = meetingRecord;
    }

    public void update(String reason, String opinion) {
        if (reason != null) {
            this.reason = reason;
        }
        if (opinion != null) {
            this.opinion = opinion;
        }
    }

    public void delete() {
        this.isDeleted = true;
    }
}
