package geumjeongyahak.domain.users.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.enums.WorkAssignmentType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 업무분장 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserWorkAssignmentResponse(
    @Schema(description = "업무분장 유형", allowableValues = { "DEPARTMENT", "HOMEROOM" }, example = "HOMEROOM")
    WorkAssignmentType type,

    @Schema(description = "업무분장 화면 표시명. 담임이면 '담임', 부서 업무면 부서명입니다.", example = "담임")
    String name,

    @Schema(description = "부서 업무분장의 부서 ID", example = "2", nullable = true)
    Long departmentId,

    @Schema(description = "담임 업무분장의 분반 ID", example = "1", nullable = true)
    Long classroomId,

    @Schema(description = "담임 업무분장의 분반명", example = "국화반", nullable = true)
    String classroomName
) {
    public static UserWorkAssignmentResponse from(User user) {
        if (user.getHomeroomClassroom() != null) {
            return new UserWorkAssignmentResponse(
                WorkAssignmentType.HOMEROOM,
                "담임",
                null,
                user.getHomeroomClassroom().getId(),
                user.getHomeroomClassroom().getName()
            );
        }
        if (user.getDepartment() != null) {
            return new UserWorkAssignmentResponse(
                WorkAssignmentType.DEPARTMENT,
                user.getDepartment().getName(),
                user.getDepartment().getId(),
                null,
                null
            );
        }
        return null;
    }
}
