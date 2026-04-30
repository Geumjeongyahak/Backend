package geumjeongyahak.domain.comment.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.channel.annotation.RequireChannelAccess;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.comment.service.CommentCrudService;
import geumjeongyahak.domain.comment.v1.dto.request.CreateCommentRequest;
import geumjeongyahak.domain.comment.v1.dto.response.CommentResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels/{channelId}/posts/{postId}/comments")
@Tag(
        name = "Comment",
        description = """
                게시글 하위 댓글 관리 API입니다.
                게시글 상세 화면에서 댓글을 작성하거나 목록을 불러오고, 작성자 본인 또는 관리자 권한으로 삭제할 때 사용합니다.
                """
)
public class CommentController {

    private final CommentCrudService commentCrudService;

    @RequireChannelAccess(minLevel = ChannelAccessLevel.READ_COMMENT)
    @Operation(
            summary = "댓글 생성",
            description = """
                    특정 게시글 하위에 댓글을 생성합니다.

                    동작 방식:
                    - parentCommentId를 비우면 일반 댓글, 값을 주면 답글로 생성됩니다.
                    - 채널 accessLevel이 READ_COMMENT 이상이어야 하며, 댓글이 허용된 게시글에만 작성 가능합니다.

                    사이드 이펙트:
                    - comments 테이블에 새 레코드가 생성됩니다.
                    """
    )
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentCrudService.createComment(channelId, postId, userDetails, request));
    }

    @RequireChannelAccess(minLevel = ChannelAccessLevel.READ_ONLY)
    @Operation(
            summary = "댓글 목록 조회",
            description = """
                    특정 게시글의 댓글 목록을 조회합니다.

                    동작 방식:
                    - 삭제되지 않은 댓글만 생성 순서대로 반환합니다.
                    - parentCommentId를 이용해 일반 댓글과 답글을 구분할 수 있습니다.

                    사이드 이펙트:
                    - 읽기 전용이며 댓글이나 게시글 상태를 변경하지 않습니다.
                    """
    )
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long channelId,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(commentCrudService.getComments(channelId, postId));
    }

    @RequireChannelAccess(minLevel = ChannelAccessLevel.READ_ONLY)
    @Operation(
            summary = "댓글 삭제",
            description = """
                    특정 게시글의 댓글을 소프트 삭제합니다.

                    동작 방식:
                    - 작성자 본인 또는 관리자/매니저만 삭제할 수 있습니다.
                    - 물리 삭제가 아니라 isDeleted=true로 처리됩니다.

                    사이드 이펙트:
                    - 이후 댓글 목록 조회에서 해당 댓글은 제외됩니다.
                    """
    )
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentCrudService.deleteComment(channelId, postId, commentId, userDetails);
        return ResponseEntity.noContent().build();
    }
}
