package geumjeongyahak.domain.request.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidLessonExchangeProposalInput;
import geumjeongyahak.domain.request.enums.LessonExchangeScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@ValidLessonExchangeProposalInput
public record CreateLessonExchangeProposalRequest(

    @Schema(description = "제안 수업 날짜", example = "2026-06-10")
    LocalDate lessonDate,

    @Schema(description = "제안 시작 교시", example = "1")
    Integer startPeriod,

    @Schema(description = "제안 종료 교시", example = "2")
    Integer endPeriod,

    @NotBlank
    @Schema(description = "제안 내용", example = "해당 날짜 수업으로 교환 가능합니다.")
    String content
) {}
