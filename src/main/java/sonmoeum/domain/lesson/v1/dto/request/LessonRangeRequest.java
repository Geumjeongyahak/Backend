package sonmoeum.domain.lesson.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import sonmoeum.common.validation.annotation.ValidLessonRange;

@ValidLessonRange
public record LessonRangeRequest(
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "시작 일자", example = "2026-02-12")
    LocalDate from,

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "종료 일자", example = "2026-02-14")
    LocalDate to
) {}
