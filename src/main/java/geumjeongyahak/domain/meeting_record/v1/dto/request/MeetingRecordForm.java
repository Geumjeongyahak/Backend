package geumjeongyahak.domain.meeting_record.v1.dto.request;

import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeetingRecordForm {
    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String agenda;

    private String discussion;
    private String suggestion;
    private MeetingRecordStatus status = MeetingRecordStatus.BEFORE_MEETING;
}
