package geumjeongyahak.domain.post.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import geumjeongyahak.domain.post.service.PostActionService.PublishPostCommand;

@Schema(description = "게시글 발행 요청 DTO입니다. 제목과 본문은 발행 시 필수입니다.")
public record PublishPostRequest(
        @Schema(description = "게시글 제목입니다.", example = "4월 운영 공지")
        @NotBlank(message = "발행 시 제목은 필수입니다.")
        @Size(min = 2, max = 255, message = "제목은 2자 이상 255자 이하로 입력해주세요.")
        String title,

        @Schema(description = "게시글 본문 HTML입니다.", example = "<p>공지 내용입니다.</p>")
        @NotBlank(message = "발행 시 본문은 필수입니다.")
        String contentHtml,

        @Schema(description = "댓글 허용 여부입니다.", example = "true", nullable = true)
        Boolean allowComment,

        @Schema(description = "게시글 대표 썸네일 URL입니다.", example = "https://cdn.example.com/thumb.png", nullable = true)
        String thumbnailUrl
) {
    public PublishPostCommand toCommand() {
        return new PublishPostCommand(title, contentHtml, allowComment, thumbnailUrl);
    }
}
