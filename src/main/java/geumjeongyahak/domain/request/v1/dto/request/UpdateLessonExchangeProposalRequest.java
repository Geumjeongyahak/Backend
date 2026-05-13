package geumjeongyahak.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdateLessonExchangeProposalRequest(

    @Schema(description = "제안 수업 날짜", example = "2026-06-17")
    LocalDate lessonDate,

    @NotBlank
    @Schema(description = "제안 내용", example = "해당 날짜 수업으로 교환 가능합니다.")
    String content
) {}
