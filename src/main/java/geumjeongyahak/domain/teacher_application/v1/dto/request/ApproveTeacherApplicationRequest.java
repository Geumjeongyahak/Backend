package geumjeongyahak.domain.teacher_application.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "교원 신청 승인 요청")
public record ApproveTeacherApplicationRequest(

    @Schema(description = "승인 메모", example = "면접 후 승인")
    String note
) {
}
