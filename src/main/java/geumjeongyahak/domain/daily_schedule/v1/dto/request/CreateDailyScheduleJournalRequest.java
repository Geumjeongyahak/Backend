package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

public record CreateDailyScheduleJournalRequest(
    @NotNull
    @DateTimeFormat(iso = DATE)
    @Schema(description = "수업 일지를 작성할 수업 날짜", example = "2026-06-20")
    LocalDate lessonDate,

    @NotNull
    @Schema(description = "수업 일지를 작성할 분반 ID", example = "1")
    Long classroomId,

    @NotNull
    @Schema(description = "개인정보 활용 동의 여부", example = "true")
    Boolean personalInfoConsent,

    @Pattern(regexp = "\\d{6}", message = "주민번호 앞자리는 숫자 6자리여야 합니다.")
    @Schema(description = "주민번호 앞자리", example = "900101")
    String residentRegistrationNumberPrefix,

    @Valid
    @NotEmpty
    @Schema(description = "교시별 수업 일지 내용")
    List<UpdateDailyScheduleJournalRequest.LessonJournalRequest> lessonJournals
) {
}
