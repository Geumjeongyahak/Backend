package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record UpdateDailyScheduleJournalRequest(
    @NotNull
    @Schema(description = "개인정보 활용 동의 여부", example = "true")
    Boolean personalInfoConsent,

    @Pattern(regexp = "\\d{6}", message = "주민번호 앞자리는 숫자 6자리여야 합니다.")
    @Schema(description = "주민번호 앞자리", example = "900101")
    String residentRegistrationNumberPrefix,

    @Valid
    @NotEmpty
    @Schema(description = "교시별 수업 일지 내용")
    List<LessonJournalRequest> lessonJournals
) {

    public record LessonJournalRequest(
        @NotNull
        @Schema(description = "수업 ID", example = "1")
        Long lessonId,

        @NotBlank
        @Schema(description = "수업 일지 내용", example = "1교시에는 국어 읽기 활동을 진행했습니다.")
        String note
    ) {
    }
}
