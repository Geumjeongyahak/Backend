package geumjeongyahak.domain.post.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.post.service.PostCrudService;
import geumjeongyahak.domain.post.v1.dto.request.CreatePostRequest;
import geumjeongyahak.domain.post.v1.dto.request.PostSearchRequest;
import geumjeongyahak.domain.post.v1.dto.request.UpdatePostRequest;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;
import geumjeongyahak.domain.post.v1.dto.response.PostSummaryResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels/{channelId}/posts")
@Tag(
        name = "Post",
        description = """
                특정 채널 하위에서 게시글을 생성, 조회, 수정, 삭제하는 API입니다.
                공지사항 채널, 반 게시판, 부서 게시판 같은 실제 게시판 화면의 글 단위를 다룰 때 사용합니다.
                채널 자체 설정은 Channel API가 담당하고, 이 API는 해당 채널 안의 게시글 데이터만 관리합니다.
                """
)
public class PostController {
    private final PostCrudService postCrudService;

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "게시글 생성",
            description = """
                    특정 채널 하위에 새 게시글을 생성합니다.

                    동작 방식:
                    - channelId는 경로로 전달되며, 요청 본문에는 포함하지 않습니다.
                    - 작성 권한은 채널의 accessLevel과 permission 규칙을 함께 기준으로 검증됩니다.
                    - 게시글 생성 직후 상세 응답이 반환됩니다.

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

    @PreAuthorize("@channelAccess.can('read', #channelId, principal)")
    @Operation(
            summary = "게시글 목록 조회",
            description = """
                    특정 채널에 속한 게시글 목록을 페이지네이션하여 조회합니다.

                    동작 방식:
                    - 고정 글이 먼저 정렬되고, 그다음 최신 생성 글 순으로 내려옵니다.

                    사이드 이펙트:
                    - 읽기 전용입니다. 조회수는 상세 조회 시점에만 증가합니다.
                    """
    )
    @GetMapping
    public ResponseEntity<PaginationResponse<PostSummaryResponse>> getPosts(
            @PathVariable Long channelId,
            @ParameterObject @Valid PostSearchRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postCrudService.getPosts(channelId, userDetails, request));
    }

    @PreAuthorize("@channelAccess.can('read', #channelId, principal)")
    @Operation(
            summary = "게시글 상세 조회",
            description = """
                    특정 채널에 속한 게시글 상세 정보를 조회합니다.

                    사이드 이펙트:
                    - 조회 성공 시 해당 게시글의 조회수가 1 증가합니다.
                    """
    )
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(
            @PathVariable Long channelId,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(postCrudService.getPost(channelId, postId));
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
        return ResponseEntity.ok(
                postCrudService.updatePost(channelId, userDetails, postId, request)
        );
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
