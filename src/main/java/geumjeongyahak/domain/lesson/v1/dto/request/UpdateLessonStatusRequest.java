package geumjeongyahak.domain.lesson.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import geumjeongyahak.domain.lesson.enums.LessonStatus;

public record UpdateLessonStatusRequest(
    @NotNull
    @Schema(description = "수업 상태", example = "COMPLETED", allowableValues = {"SCHEDULED", "COMPLETED", "CANCELED"})
    LessonStatus status
) {}
