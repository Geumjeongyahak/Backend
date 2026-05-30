package geumjeongyahak.domain.meeting_record.v1.dto.response;

import geumjeongyahak.domain.meeting_record.entity.MeetingAbsenceReport;
import java.time.LocalDateTime;

public record MeetingAbsenceReportResponse(
    Long id,
    Long authorId,
    String author,
    String reason,
    String opinion,
    LocalDateTime createdAt
) {
    public static MeetingAbsenceReportResponse from(MeetingAbsenceReport report) {
        return new MeetingAbsenceReportResponse(
            report.getId(),
            report.getAuthor().getId(),
            report.getAuthor().getName(),
            report.getReason(),
            report.getOpinion(),
            report.getCreatedAt()
        );
    }
}
