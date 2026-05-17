package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateDailyStudentAttendancesRequest(
    @NotEmpty
    @Schema(description = "학생 출석 정보", example = "[{\"studentId\": 1, \"status\": \"PRESENT\"}]")
    List<@Valid UpdateDailyStudentAttendanceItemRequest> attendances
) {}
