package geumjeongyahak.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UpdateLessonExchangeProposalRequest(

    @Schema(description = "제안 수업일. 입력하면 로그인 사용자의 해당 날짜 DailySchedule을 내놓는 교환형(EXCHANGE), 생략하면 대체형(SUBSTITUTION)으로 처리됩니다.", example = "2026-06-11")
    LocalDate lessonDate,

    @NotBlank
    @Schema(description = "제안 내용", example = "해당 날짜 수업으로 교환 가능합니다.")
    String content
) {}
