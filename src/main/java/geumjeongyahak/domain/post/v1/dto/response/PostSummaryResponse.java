package geumjeongyahak.domain.post.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.post.entity.Post;

@Schema(
        description = """
                게시글 목록 응답 DTO입니다.
                게시판 리스트 화면에서 필요한 최소 정보만 담아 전송합니다.
                제목, 작성자, 채널 정보, 공지 여부, 조회수 같은 목록 렌더링용 요약 데이터에 집중합니다.
                """
)
public record PostSummaryResponse(
        @Schema(description = "게시글 내부 식별자입니다.", example = "1")
        Long id,

        @Schema(description = "이 게시글이 속한 채널 ID입니다.", example = "2")
        Long channelId,

        @Schema(description = "목록 화면에 함께 표시할 채널 이름입니다.", example = "공지사항")
        String channelName,

        @Schema(description = "채널 유형입니다. 전체/반/부서 게시판 구분에 사용할 수 있습니다.", example = "ALL")
        String channelType,

        @Schema(description = "게시글 제목입니다.", example = "4월 운영 공지")
        String title,

        @Schema(description = "게시글 유형입니다.", example = "NOTICE")
        String postType,

        @Schema(description = "게시글 상태입니다.", example = "PUBLISHED")
        String status,

        @Schema(description = "작성자 사용자 ID입니다.", example = "1")
        Long authorId,

        @Schema(description = "작성자 표시 이름입니다.", example = "Administrator")
        String authorName,

        @Schema(description = "상단 고정 여부입니다. 목록 정렬과 강조 표시 기준으로 활용할 수 있습니다.", example = "false")
        boolean isPinned,

        @Schema(description = "누적 조회수입니다. 목록에서 인기 글 판단이나 우측 정보 컬럼에 활용할 수 있습니다.", example = "146")
        long viewCount
) {
    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getChannel().getId(),
                post.getChannel().getName(),
                post.getChannel().getChannelType().name(),
                post.getTitle(),
                post.getPostType().name(),
                post.getStatus().name(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.isPinned(),
                post.getViewCount()
        );
    }
}
