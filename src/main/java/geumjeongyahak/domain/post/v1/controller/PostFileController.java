package geumjeongyahak.domain.post.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.post.service.PostFileService;
import geumjeongyahak.domain.post.v1.dto.request.AttachPostFileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels/{channelId}/posts")
@Tag(name = "Post File", description = "게시글 이미지 및 첨부파일 API")
public class PostFileController {

    private final PostFileService postFileService;

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "게시글 이미지 업로드 및 연동",
            description = """
                    이미지를 업로드하고 게시글에 즉시 연동합니다.

                    제약 사항:
                    - 본인이 작성한 초안에만 연동할 수 있습니다.
                    - DRAFT 상태 게시글에만 사용할 수 있습니다.
                    """
    )
    @PostMapping(value = "/{postId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> attachImage(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postFileService.attachImage(channelId, postId, userDetails, file));
    }

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "게시글 첨부파일 업로드 및 연동",
            description = """
                    첨부파일을 업로드하고 게시글에 즉시 연동합니다.

                    제약 사항:
                    - 본인이 작성한 초안에만 연동할 수 있습니다.
                    - DRAFT 상태 게시글에만 사용할 수 있습니다.
                    """
    )
    @PostMapping(value = "/{postId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> attachAttachment(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postFileService.attachAttachment(channelId, postId, userDetails, file));
    }

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "등록된 파일을 게시글 첨부파일로 연동",
            description = """
                    files 테이블에 이미 등록된 파일을 게시글 첨부파일로 연동합니다.

                    사용 사례:
                    - /api/v1/files/drive 로 Google Drive 파일 메타데이터를 등록한 뒤 fileId로 연동합니다.
                    - 기존 첨부파일 응답 구조는 PostAttachmentInfo와 동일하게 유지됩니다.

                    제약 사항:
                    - 본인이 작성한 초안에만 연동할 수 있습니다.
                    - DRAFT 상태 게시글에만 사용할 수 있습니다.
                    """
    )
    @PostMapping(value = "/{postId}/attachments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileUploadResponse> attachRegisteredAttachment(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @Valid @RequestBody AttachPostFileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postFileService.attachRegisteredAttachment(
                channelId,
                postId,
                userDetails,
                request.fileId(),
                request.sortOrder()));
    }

    @PreAuthorize("@channelAccess.can('write', #channelId, principal)")
    @Operation(
            summary = "게시글 첨부파일 삭제",
            description = """
                    게시글에 연동된 첨부파일을 삭제합니다.

                    제약 사항:
                    - 본인이 작성한 초안에만 사용할 수 있습니다.
                    - DRAFT 상태 게시글에만 사용할 수 있습니다.
                    """
    )
    @DeleteMapping("/{postId}/attachments/{fileId}")
    public ResponseEntity<Void> detachAttachment(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postFileService.detachAttachment(channelId, postId, userDetails, fileId);
        return ResponseEntity.noContent().build();
    }
}
