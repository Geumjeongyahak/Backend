package geumjeongyahak.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateLessonExchangeRequestRequest(

    @NotNull
    @Schema(description = "교환 대상 수업 날짜", example = "2026-06-10")
    LocalDate lessonDate,

    @NotBlank
    @Schema(description = "요청 제목", example = "수업 교환 요청")
    String title,

    @NotBlank
    @Schema(description = "요청 내용", example = "사정으로 인해 교환을 요청합니다.")
    String content,

    @NotNull
    @Future
    @Schema(description = "요청 만료 시각", example = "2026-06-07T22:00:00")
    LocalDateTime expiresAt
) {}
