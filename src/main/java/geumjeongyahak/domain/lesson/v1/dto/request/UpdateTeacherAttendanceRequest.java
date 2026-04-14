package geumjeongyahak.domain.lesson.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import geumjeongyahak.domain.lesson.enums.TeacherAttendanceStatus;

public record UpdateTeacherAttendanceRequest(
    @NotNull
    @Schema(
        description = "교사 출석 상태",
        example = "PRESENT",
        allowableValues = {"PRESENT", "ABSENT", "LATE"}
    )
    TeacherAttendanceStatus status
) {}
