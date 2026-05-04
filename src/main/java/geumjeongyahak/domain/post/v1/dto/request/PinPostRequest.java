package geumjeongyahak.domain.post.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "게시글 고정 설정 요청 DTO입니다.")
public record PinPostRequest(
        @Schema(description = "상단 고정 여부입니다. true이면 고정, false이면 고정 해제합니다.", example = "true")
        @NotNull(message = "isPinned는 필수입니다.")
        Boolean isPinned
) {
}
