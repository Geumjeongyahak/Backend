package geumjeongyahak.domain.lesson.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateLessonNoteRequest(
    @NotBlank
    @Schema(description = "수업일지(메모)", example = "오늘 진도: ~ / 특이사항: ~")
    String note
) {}
