package geumjeongyahak.domain.users.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "사용자 대표 분반 지정 요청 DTO")
public record AssignUserClassroomRequest(
    @Schema(description = "사용자에게 대표 분반으로 지정할 분반 ID", example = "1")
    @NotNull(message = "분반 ID는 필수입니다.")
    Long classroomId
) {
}
