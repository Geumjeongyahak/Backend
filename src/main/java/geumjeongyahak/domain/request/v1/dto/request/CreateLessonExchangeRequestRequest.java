package geumjeongyahak.domain.request.v1.dto.request;

import geumjeongyahak.domain.request.enums.LessonExchangeScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateLessonExchangeRequestRequest(

    @NotNull
    @Schema(description = "교환 대상 수업 ID", example = "1")
    Long lessonId,

    @NotBlank
    @Schema(description = "요청 제목", example = "수업 교환 요청")
    String title,

    @NotBlank
    @Schema(description = "요청 내용", example = "사정으로 인해 교환을 요청합니다.")
    String content,

    @NotNull
    @Schema(description = "교환 범위", example = "PARTIAL")
    LessonExchangeScope scope,

    @Schema(description = "교환 시작 기간", example = "1")
    Integer startPeriod,

    @Schema(description = "교환 종료 기간", example = "2")
    Integer endPeriod,

    @NotNull
    @Schema(description = "요청 만료 시간", example = "2026-06-01T22:00:00")
    LocalDateTime expiresAt
) {}
