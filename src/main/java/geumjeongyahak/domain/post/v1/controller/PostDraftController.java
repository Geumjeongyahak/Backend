package geumjeongyahak.domain.post.v1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.post.service.PostActionService;
import geumjeongyahak.domain.post.v1.dto.request.SaveDraftRequest;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;
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

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "초안 이미지 업로드 및 연동",
            description = """
                    이미지를 업로드하고 초안 게시글에 즉시 연동합니다.

                    동작 방식:
                    - multipart/form-data로 이미지 파일을 전송하면 스토리지 업로드와 post_files 매핑이 한 번에 처리됩니다.
                    - 에디터에서 이미지 삽입 시 반환된 url을 본문 HTML에 직접 사용할 수 있습니다.
                    - 동일 파일이 이미 연동되어 있으면 중복 저장 없이 업로드 결과만 반환합니다.

                    제약 사항:
                    - 본인이 작성한 초안에만 연동할 수 있습니다.
                    - DRAFT 상태 게시글에만 사용할 수 있습니다.
                    """
    )
    @PostMapping(value = "/{postId}/draft/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> attachImage(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postActionService.attachImageToDraft(channelId, postId, userDetails, file));
    }

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "초안 첨부파일 업로드 및 연동",
            description = """
                    첨부파일을 업로드하고 초안 게시글에 즉시 연동합니다.

                    동작 방식:
                    - multipart/form-data로 파일을 전송하면 스토리지 업로드와 post_attachments 매핑이 한 번에 처리됩니다.
                    - 동일 파일이 이미 연동되어 있으면 중복 저장 없이 업로드 결과만 반환합니다.

                    제약 사항:
                    - 본인이 작성한 초안에만 연동할 수 있습니다.
                    - DRAFT 상태 게시글에만 사용할 수 있습니다.
                    """
    )
    @PostMapping(value = "/{postId}/draft/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> attachAttachment(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postActionService.attachAttachmentToDraft(channelId, postId, userDetails, file));
    }
}
