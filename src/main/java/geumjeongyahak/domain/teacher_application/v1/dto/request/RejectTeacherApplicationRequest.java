package geumjeongyahak.domain.teacher_application.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "교원 신청 반려 요청")
public record RejectTeacherApplicationRequest(

    @NotBlank(message = "반려 사유는 필수입니다.")
    @Schema(description = "반려 사유", example = "면접 일정 미참석")
    String note
) {
}
