package geumjeongyahak.domain.teacher_assignment.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "관리자 교사 시간표 배정 해제 요청")
public record UnassignTeacherScheduleRequest(

    @NotEmpty(message = "배정 해제할 시간표 과목 ID 목록은 필수입니다.")
    @Schema(description = "배정 해제할 시간표 과목 ID 목록", example = "[1, 2]")
    List<@NotNull Long> subjectIds
) {
}
