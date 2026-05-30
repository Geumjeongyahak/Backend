package geumjeongyahak.domain.meeting_record.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMeetingRecordRequest(
    @NotBlank
    @Size(max = 255)
    String title,

    @NotBlank
    String agenda
) {
}
