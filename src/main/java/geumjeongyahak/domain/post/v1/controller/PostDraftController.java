package geumjeongyahak.domain.post.v1.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.post.service.PostActionService;
import geumjeongyahak.domain.post.service.PostCrudService;
import geumjeongyahak.domain.post.v1.dto.request.SaveDraftRequest;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;
import geumjeongyahak.domain.post.v1.dto.response.PostSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels/{channelId}/posts")
@Tag(name = "Post", description = "채널 내 게시글 API")
public class PostDraftController {

    private final PostActionService postActionService;
    private final PostCrudService postCrudService;

    @PreAuthorize("@channelAccess.can('read', #channelId, principal)")
    @Operation(
            summary = "내 초안 목록 조회",
            description = """
                    현재 로그인한 사용자가 해당 채널에 작성 중인 DRAFT 게시글 목록을 반환합니다.

                    동작 방식:
                    - 본인이 작성한 DRAFT 상태 게시글만 반환합니다.
                    - 최신 생성 순으로 정렬됩니다.
                    """
    )
    @GetMapping("/me/drafts")
    public ResponseEntity<PaginationResponse<PostSummaryResponse>> getMyDrafts(
            @PathVariable Long channelId,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postCrudService.getMyDrafts(channelId, userDetails, pageable));
    }

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "게시글 초안 생성",
            description = """
                    특정 채널 하위에 빈 초안 게시글을 생성합니다.

                    사용 시점:
                    - 프론트엔드가 '게시글 작성' 버튼을 눌렀을 때 편집 화면 진입용 URL을 만들기 위해 먼저 호출합니다.

                    동작 방식:
                    - 제목과 본문이 비어 있는 DRAFT 게시글을 생성합니다.
                    - 만료 시각은 서버 설정(app.post.draft-expiration-minutes) 기준으로 자동 계산됩니다.

                    사이드 이펙트:
                    - posts 테이블에 status=DRAFT 레코드가 생성됩니다.
                    - 아직 발행 전이므로 채널 최근 게시 시각(lastPostedAt)은 갱신하지 않습니다.
                    """
    )
    @PostMapping("/drafts")
    public ResponseEntity<PostDetailResponse> createDraft(
            @PathVariable Long channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postActionService.createDraft(channelId, userDetails));
    }

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "초안 임시 저장",
            description = """
                    초안 게시글의 내용을 임시 저장합니다.

                    동작 방식:
                    - 전달한 필드만 갱신되며, null인 필드는 기존 값을 유지합니다.
                    - 저장 시 만료 시각이 서버 설정(app.post.draft-expiration-minutes) 기준으로 자동 연장됩니다.
                    - 상태는 항상 DRAFT로 유지됩니다.

                    제약 사항:
                    - 본인이 작성한 초안만 저장할 수 있습니다.
                    - PUBLISHED 또는 ARCHIVED 상태인 게시글에는 사용할 수 없습니다.
                    """
    )
    @PutMapping("/{postId}/draft")
    public ResponseEntity<PostDetailResponse> saveDraft(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @Valid @RequestBody SaveDraftRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postActionService.saveDraft(channelId, postId, userDetails, request.toCommand()));
    }

}
