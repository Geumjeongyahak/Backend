package geumjeongyahak.domain.meeting_record.v1.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateAbsenceReportRequest(
    @NotBlank
    String reason,
    String opinion
) {
}
