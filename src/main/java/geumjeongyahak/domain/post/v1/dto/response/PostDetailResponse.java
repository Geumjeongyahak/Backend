package geumjeongyahak.domain.post.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.post.entity.Post;

import java.time.LocalDateTime;

@Schema(
        description = """
                게시글 상세 응답 DTO입니다.
                게시판 상세 화면이나 수정 화면 초기값 구성에 필요한 정보를 담습니다.
                제목, 본문, 작성자, 채널 메타데이터, 조회수, 생성/수정 시각까지 함께 제공합니다.
                """
)
public record PostDetailResponse(
        @Schema(description = "게시글 내부 식별자입니다.", example = "1")
        Long id,

        @Schema(description = "이 게시글이 속한 채널 ID입니다.", example = "2")
        Long channelId,

        @Schema(description = "게시글이 속한 채널의 표시 이름입니다.", example = "공지사항")
        String channelName,

        @Schema(description = "채널 유형입니다. 화면에서 전역/반/부서 게시판 구분에 사용할 수 있습니다.", example = "ALL")
        String channelType,

        @Schema(description = "게시글 제목입니다.", example = "4월 운영 공지")
        String title,

        @Schema(description = "게시글 본문 HTML입니다. 실제 렌더링 대상이 되는 상세 내용입니다.", example = "<p>공지 내용입니다.</p>")
        String contentHtml,

        @Schema(description = "게시글 유형입니다.", example = "NOTICE")
        String postType,

        @Schema(description = "게시글 상태입니다.", example = "PUBLISHED")
        String status,

        @Schema(description = "작성자 사용자 ID입니다.", example = "1")
        Long authorId,

        @Schema(description = "작성자 표시 이름입니다.", example = "Administrator")
        String authorName,

        @Schema(description = "상단 고정 여부입니다. true면 같은 게시판 내에서 우선 노출됩니다.", example = "false")
        boolean isPinned,

        @Schema(description = "댓글 허용 여부입니다. 상세 화면에서 댓글 UI 노출 조건으로 사용할 수 있습니다.", example = "true")
        boolean allowComment,

        @Schema(description = "누적 조회수입니다. 상세 조회 성공 시 증가합니다.", example = "146")
        long viewCount,

        @Schema(description = "게시글 생성 시각입니다. 서버가 자동 기록합니다.", example = "2026-04-10T19:30:00")
        LocalDateTime createdAt,

        @Schema(description = "마지막 수정 시각입니다. 수정이 없으면 생성 시각과 같을 수 있습니다.", example = "2026-04-10T19:30:00")
        LocalDateTime updatedAt
) {
    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getChannel().getId(),
                post.getChannel().getName(),
                post.getChannel().getChannelType().name(),
                post.getTitle(),
                post.getContentHtml(),
                post.getPostType().name(),
                post.getStatus().name(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.isPinned(),
                post.isAllowComment(),
                post.getViewCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
