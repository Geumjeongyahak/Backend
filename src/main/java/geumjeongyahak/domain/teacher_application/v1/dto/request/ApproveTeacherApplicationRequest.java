package geumjeongyahak.domain.teacher_application.v1.dto.request;

import java.time.LocalDate;

import geumjeongyahak.common.validation.annotation.ValidTeacherApplicationApprovalPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "교원 신청 승인 요청")
@ValidTeacherApplicationApprovalPeriod
public record ApproveTeacherApplicationRequest(

    @NotNull(message = "분반 ID는 필수입니다.")
    @Schema(description = "배정할 분반 ID", example = "1")
    Long classroomId,

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
