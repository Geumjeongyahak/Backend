package geumjeongyahak.domain.post.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.post.service.PostCrudService;
import geumjeongyahak.domain.post.v1.dto.request.CreatePostRequest;
import geumjeongyahak.domain.post.v1.dto.request.PinPostRequest;
import geumjeongyahak.domain.post.v1.dto.request.UpdatePostRequest;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels/{channelId}/posts")
@Tag(name = "Post", description = "채널 내 게시글 API")
public class PostWriteController {

    private final PostCrudService postCrudService;

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "게시글 생성",
            description = """
                    특정 채널 하위에 새 게시글을 생성합니다.

                    동작 방식:
                    - channelId는 경로로 전달되며, 요청 본문에는 포함하지 않습니다.
                    - 작성 권한은 채널의 accessLevel과 permission 규칙을 함께 기준으로 검증됩니다.

                    사이드 이펙트:
                    - posts 테이블에 새 레코드가 생성됩니다.
                    - 해당 채널의 최근 게시 시각(lastPostedAt)이 함께 갱신됩니다.
                    - 기본 상태를 따로 주지 않으면 PUBLISHED로 처리됩니다.
                    """
    )
    @PostMapping
    public ResponseEntity<PostDetailResponse> createPost(
            @PathVariable Long channelId,
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                postCrudService.createPost(channelId, userDetails, request)
        );
    }

    @PreAuthorize("@channelAccess.can('manage', #channelId, principal) or @postAccess.can(#postId, principal)")
    @Operation(
            summary = "게시글 수정",
            description = """
                    특정 채널에 속한 게시글을 수정합니다.

                    동작 방식:
                    - 작성자 본인 또는 관리자/매니저만 수정할 수 있습니다.
                    - 변경값이 하나도 없으면 오류가 반환됩니다.

                    사이드 이펙트:
                    - 채널의 최근 게시 시각(lastPostedAt)이 다시 계산되어 갱신됩니다.
                    """
    )
    @PutMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> updatePost(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postCrudService.updatePost(channelId, userDetails, postId, request));
    }

    @PreAuthorize("@channelAccess.can('manage', #channelId, principal)")
    @Operation(
            summary = "게시글 고정/고정 해제",
            description = """
                    게시글의 상단 고정 여부를 설정합니다.

                    동작 방식:
                    - isPinned=true이면 게시판 목록 최상단에 고정됩니다.
                    - isPinned=false이면 고정이 해제됩니다.

                    제약 사항:
                    - 채널 관리 권한(manage)이 있어야 합니다.
                    """
    )
    @PutMapping("/{postId}/pin")
    public ResponseEntity<PostDetailResponse> pinPost(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @Valid @RequestBody PinPostRequest request
    ) {
        return ResponseEntity.ok(postCrudService.pinPost(channelId, postId, request.isPinned()));
    }

    @PreAuthorize("@channelAccess.can('manage', #channelId, principal) or @postAccess.can(#postId, principal)")
    @Operation(
            summary = "게시글 삭제",
            description = """
                    특정 채널에 속한 게시글을 소프트 삭제합니다.

                    동작 방식:
                    - 물리 삭제가 아니라 isDeleted=true로 처리됩니다.
                    - 작성자 본인 또는 관리자/매니저만 삭제할 수 있습니다.

                    사이드 이펙트:
                    - 채널의 최근 게시 시각(lastPostedAt)이 남아 있는 최신 게시글 기준으로 다시 계산됩니다.
                    """
    )
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postCrudService.deletePost(channelId, userDetails, postId);
        return ResponseEntity.noContent().build();
    }
}
