package geumjeongyahak.domain.post.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.post.service.PostActionService;
import geumjeongyahak.domain.post.v1.dto.request.PublishPostRequest;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels/{channelId}/posts")
@Tag(name = "Post", description = "채널 내 게시글 API")
public class PostPublishController {

    private final PostActionService postActionService;

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "초안 발행",
            description = """
                    초안 게시글을 발행합니다.

                    동작 방식:
                    - DRAFT 상태 게시글을 PUBLISHED로 전환합니다.
                    - 제목과 본문은 발행 시점에 반드시 전달해야 합니다.
                    - expiresAt은 발행과 동시에 자동으로 제거됩니다.
                    - 썸네일을 지정하지 않으면 연동된 이미지 중 첫 번째(sortOrder 기준)가 자동으로 설정됩니다.

                    사이드 이펙트:
                    - 채널의 최근 게시 시각(lastPostedAt)이 갱신됩니다.
                    - PostPublishedEvent, PostChangedEvent가 발행됩니다.

                    제약 사항:
                    - 본인이 작성한 초안만 발행할 수 있습니다.
                    - DRAFT 상태 게시글에만 사용할 수 있습니다.
                    """
    )
    @PutMapping("/{postId}/publish")
    public ResponseEntity<PostDetailResponse> publish(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @Valid @RequestBody PublishPostRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postActionService.publish(channelId, postId, userDetails, request.toCommand()));
    }
}
