package geumjeongyahak.domain.comment.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.comment.entity.Comment;

import java.time.LocalDateTime;

@Schema(
        description = """
                댓글 응답 DTO입니다.
                댓글 목록 화면이나 게시글 상세 하단의 댓글 영역 구성에 필요한 작성자, 본문, 부모 댓글 관계 정보를 제공합니다.
                """
)
public record CommentResponse(
        @Schema(description = "댓글 내부 식별자입니다.", example = "21")
        Long id,

        @Schema(description = "댓글이 속한 게시글 ID입니다.", example = "3")
        Long postId,

        @Schema(description = "부모 댓글 ID입니다. 일반 댓글이면 null입니다.", example = "12", nullable = true)
        Long parentCommentId,

        @Schema(description = "댓글 작성자 사용자 ID입니다.", example = "7")
        Long authorId,

        @Schema(description = "댓글 작성자 표시 이름입니다.", example = "김민수")
        String authorName,

        @Schema(description = "댓글 본문입니다.", example = "확인했습니다.")
        String content,

        @Schema(description = "댓글 상태입니다.", example = "ACTIVE")
        String status,

        @Schema(description = "댓글 생성 시각입니다.", example = "2026-04-10T20:45:00")
        LocalDateTime createdAt,

        @Schema(description = "댓글 수정 시각입니다.", example = "2026-04-10T20:45:00")
        LocalDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getContent(),
                comment.getStatus().name(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
