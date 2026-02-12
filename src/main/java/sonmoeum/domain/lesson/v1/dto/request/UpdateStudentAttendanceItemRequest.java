package sonmoeum.domain.lesson.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import sonmoeum.domain.lesson.enums.StudentAttendanceStatus;

public record UpdateStudentAttendanceItemRequest(
    @NotNull
    @Schema(description = "학생 ID", example = "1")
    Long studentId,

    @NotNull
    @Schema(description = "출석 상태", example = "PRESENT")
    StudentAttendanceStatus status,

    @Schema(description = "메모", example = "지각 예정")
    String memo
) {}
