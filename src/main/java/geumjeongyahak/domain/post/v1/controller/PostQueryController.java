package geumjeongyahak.domain.post.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.post.service.PostCrudService;
import geumjeongyahak.domain.post.v1.dto.request.PostSearchRequest;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;
import geumjeongyahak.domain.post.v1.dto.response.PostSummaryResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels/{channelId}/posts")
@Tag(name = "Post", description = "채널 내 게시글 API")
public class PostQueryController {

    private final PostCrudService postCrudService;

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

    @PreAuthorize("isAuthenticated()")
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
}
