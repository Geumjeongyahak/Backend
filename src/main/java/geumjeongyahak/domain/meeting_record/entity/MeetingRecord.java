package geumjeongyahak.domain.meeting_record.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@Table(name = "meeting_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String agenda;

    @Column(columnDefinition = "TEXT")
    private String discussion;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingRecordStatus status;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @OneToMany(mappedBy = "meetingRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC, id DESC")
    private List<MeetingAbsenceReport> absenceReports = new ArrayList<>();

    public MeetingRecord(@NonNull User author, @NonNull String title, @NonNull String agenda) {
        this.author = author;
        this.title = title;
        this.agenda = agenda;
        this.status = MeetingRecordStatus.BEFORE_MEETING;
        this.isDeleted = false;
        this.viewCount = 0L;
    }

    public void update(
        String title,
        String agenda,
        String discussion,
        String suggestion,
        MeetingRecordStatus status
    ) {
        if (title != null) {
            this.title = title;
        }
        if (agenda != null) {
            this.agenda = agenda;
        }
        if (discussion != null) {
            this.discussion = discussion;
        }
        if (suggestion != null) {
            this.suggestion = suggestion;
        }
        if (status != null) {
            this.status = status;
        }
    }

    public void addAbsenceReport(MeetingAbsenceReport report) {
        report.assignMeetingRecord(this);
        this.absenceReports.add(report);
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
