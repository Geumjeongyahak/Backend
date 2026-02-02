package sonmoeum.api.v1.lessons.dto.request;

import sonmoeum.domain.lesson.entity.Lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceRequest(
    @Schema(description = "출석 상태", example = "PRESENT")
    @NotNull(message = "출석 상태는 필수입니다.")
    Lesson.AttendanceStatus attendance
) {}
