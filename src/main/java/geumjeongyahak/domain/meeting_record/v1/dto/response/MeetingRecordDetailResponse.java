package geumjeongyahak.domain.meeting_record.v1.dto.response;

import geumjeongyahak.domain.meeting_record.entity.MeetingRecord;
import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingRecordDetailResponse(
    Long id,
    String title,
    Long authorId,
    String author,
    LocalDateTime createdAt,
    MeetingRecordStatus status,
    long viewCount,
    String agenda,
    String discussion,
    String suggestion,
    List<MeetingAbsenceReportResponse> absenceReports
) {
    public static MeetingRecordDetailResponse from(MeetingRecord record) {
        return new MeetingRecordDetailResponse(
            record.getId(),
            record.getTitle(),
            record.getAuthor().getId(),
            record.getAuthor().getName(),
            record.getCreatedAt(),
            record.getStatus(),
            record.getViewCount(),
            record.getAgenda(),
            record.getDiscussion(),
            record.getSuggestion(),
            record.getAbsenceReports().stream()
                .filter(report -> !report.isDeleted())
                .map(MeetingAbsenceReportResponse::from)
                .toList()
        );
    }
}
