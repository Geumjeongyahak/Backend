package geumjeongyahak.domain.meeting_record.v1.dto.request;

import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
import jakarta.validation.constraints.Size;

public record UpdateMeetingRecordRequest(
    @Size(max = 255)
    String title,
    String agenda,
    String discussion,
    String suggestion,
    MeetingRecordStatus status
) {
}
