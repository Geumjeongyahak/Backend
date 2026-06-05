package geumjeongyahak.domain.teacher_application.v1.dto.request;

import java.time.LocalDate;
import java.util.List;

import geumjeongyahak.common.validation.annotation.ValidTeacherApplicationApprovalPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "교원 신청 승인 요청")
@ValidTeacherApplicationApprovalPeriod
public record ApproveTeacherApplicationRequest(

    @NotEmpty(message = "배정 시간표 과목 ID 목록은 필수입니다.")
    @Schema(description = "배정할 시간표 과목 ID 목록. 같은 분반/요일/기간의 하루 시간표 슬롯을 지정합니다.", example = "[1, 2]")
    List<@NotNull Long> assignedSubjectIds,

    @NotNull(message = "교원 활동 시작일은 필수입니다.")
    @Schema(description = "교원 활동 시작일", example = "2026-06-01")
    LocalDate teacherStartAt,

    @NotNull(message = "교원 활동 종료일은 필수입니다.")
    @Schema(description = "교원 활동 종료일", example = "2026-12-31")
    LocalDate teacherEndAt,

    @Schema(description = "승인 메모", example = "면접 후 승인")
    String note
) {
}
