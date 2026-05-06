package geumjeongyahak.domain.post.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import geumjeongyahak.domain.post.service.PostActionService.SaveDraftCommand;

@Schema(description = "초안 임시 저장 요청 DTO입니다. 전달한 필드만 갱신되며, null인 필드는 기존 값을 유지합니다.")
public record SaveDraftRequest(
        @Schema(description = "게시글 제목입니다.", example = "4월 운영 공지", nullable = true)
        @Size(max = 255, message = "제목은 255자 이하로 입력해주세요.")
        String title,

        @Schema(description = "게시글 본문 HTML입니다.", example = "<p>내용입니다.</p>", nullable = true)
        String contentHtml,

        @Schema(description = "댓글 허용 여부입니다.", example = "true", nullable = true)
        Boolean allowComment,

        @Schema(description = "게시글 대표 썸네일 URL입니다.", example = "https://cdn.example.com/thumb.png", nullable = true)
        String thumbnailUrl
) {
    public SaveDraftCommand toCommand() {
        return new SaveDraftCommand(title, contentHtml, allowComment, thumbnailUrl);
    }
}
