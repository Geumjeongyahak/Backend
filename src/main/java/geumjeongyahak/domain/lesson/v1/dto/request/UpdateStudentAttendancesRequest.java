package geumjeongyahak.domain.lesson.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateStudentAttendancesRequest(
    @NotEmpty
    @Schema(description = "학생 출석 정보", example = "[{\"studentId\": 1, \"status\": \"PRESENT\", \"memo\": \"지각 예정\"}]")
    List<@Valid UpdateStudentAttendanceItemRequest> attendances
) {}
