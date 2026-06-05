package geumjeongyahak.domain.teacher_assignment.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "관리자 교사 시간표 배정 요청")
public record AssignTeacherToScheduleRequest(

    @NotNull(message = "교사 ID는 필수입니다.")
    @Schema(description = "배정할 교사 ID", example = "4")
    Long teacherId,

    @NotEmpty(message = "배정 시간표 과목 ID 목록은 필수입니다.")
    @Schema(description = "배정할 시간표 과목 ID 목록. 같은 분반/요일/기간의 하루 시간표 슬롯을 지정합니다.", example = "[1, 2]")
    List<@NotNull Long> subjectIds,

    @Schema(description = "기존 담당 교사가 있는 시간표 교체 확인 여부", example = "false")
    Boolean confirmTeacherReplacement
) {
    public boolean replacementConfirmed() {
        return Boolean.TRUE.equals(confirmTeacherReplacement);
    }
}
