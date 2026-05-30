package geumjeongyahak.domain.meeting_record.v1.dto.response;

import geumjeongyahak.domain.meeting_record.entity.MeetingRecord;
import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
import java.time.LocalDateTime;

public record MeetingRecordSummaryResponse(
    Long id,
    String title,
    Long authorId,
    String author,
    LocalDateTime createdAt,
    MeetingRecordStatus status,
    long viewCount
) {
    public static MeetingRecordSummaryResponse from(MeetingRecord record) {
        return new MeetingRecordSummaryResponse(
            record.getId(),
            record.getTitle(),
            record.getAuthor().getId(),
            record.getAuthor().getName(),
            record.getCreatedAt(),
            record.getStatus(),
            record.getViewCount()
        );
    }
}
