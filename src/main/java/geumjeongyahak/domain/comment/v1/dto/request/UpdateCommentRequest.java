package geumjeongyahak.domain.comment.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글 수정 요청 DTO입니다.")
public record UpdateCommentRequest(
        @Schema(description = "수정할 댓글 본문입니다.", example = "수정된 댓글입니다.")
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = 2000, message = "댓글 내용은 2000자 이하로 입력해주세요.")
        String content
) {
}
