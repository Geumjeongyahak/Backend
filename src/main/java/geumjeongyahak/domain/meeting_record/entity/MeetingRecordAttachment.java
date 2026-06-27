package geumjeongyahak.domain.meeting_record.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.file.entity.File;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@Table(name = "meeting_record_attachments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingRecordAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_record_id", nullable = false)
    private MeetingRecord meetingRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    public MeetingRecordAttachment(
        @NonNull MeetingRecord meetingRecord,
        @NonNull File file,
        Integer sortOrder
    ) {
        this.meetingRecord = meetingRecord;
        this.file = file;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }
}
