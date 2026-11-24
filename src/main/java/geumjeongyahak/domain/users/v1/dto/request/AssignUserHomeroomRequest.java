package geumjeongyahak.domain.users.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "사용자 담임 업무분장 지정 요청 DTO")
public record AssignUserHomeroomRequest(
    @Schema(description = "사용자가 담임으로 맡을 분반 ID", example = "1")
    @NotNull(message = "담임 분반 ID는 필수입니다.")
    Long classroomId
) {
}
