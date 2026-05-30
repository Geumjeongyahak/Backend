package geumjeongyahak.domain.file.v1.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.service.AttachmentUploadService;
import geumjeongyahak.domain.file.service.DriveFileService;
import geumjeongyahak.domain.file.service.ImageUploadService;
import geumjeongyahak.domain.file.v1.dto.request.RegisterDriveFileRequest;
import geumjeongyahak.domain.file.v1.dto.response.FileDownloadUrlResponse;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
@Tag(
    name = "File",
    description = "이미지 및 첨부파일 업로드 API. "
        + "프로필 이미지, 게시글 본문 이미지, 구매 증빙 이미지, 일반 첨부파일 업로드를 담당하며 "
        + "업로드된 파일 메타데이터를 files 테이블에 저장합니다. "
        + "게시글과의 연결(post_files, post_attachments)이나 접근 권한 정책은 후속 도메인 구현에서 매핑됩니다."
)
public class FileController {

    private final ImageUploadService imageUploadService;
    private final AttachmentUploadService attachmentUploadService;
    private final DriveFileService driveFileService;

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "프로필 이미지 업로드",
        description = "현재 로그인한 사용자의 프로필 이미지를 업로드합니다. "
            + "사용 시점은 마이페이지 또는 사용자 정보 수정 화면에서 프로필 사진을 변경할 때입니다. "
            + "업로드된 이미지는 서버에서 256x256 PNG로 리사이즈된 뒤 profiles 디렉터리에 저장되며, "
            + "files 테이블에는 변환된 파일의 메타데이터가 기록됩니다. "
            + "응답에는 즉시 화면에 반영할 수 있는 공개 URL과 저장된 파일 메타데이터가 포함됩니다."
    )
    @PostMapping(value = "/images/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadProfileImage(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Parameter(
            description = "프로필 이미지 파일. JPG, PNG, GIF, WEBP 형식을 허용하며 서버에서 PNG로 변환됩니다."
        )
        @RequestPart("file") MultipartFile file
    ) {
        log.debug("POST /api/v1/files/images/profile - 프로필 이미지 업로드 요청");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(imageUploadService.uploadProfileImage(userDetails.getUserId(), file));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "게시글 본문 이미지 업로드",
        description = "게시글 에디터(Tiptap, Quill 등)에서 이미지를 붙여넣거나 업로드할 때 호출합니다. "
            + "업로드된 이미지는 editor 디렉터리에 저장되고, 응답으로 반환되는 URL을 에디터의 img src에 즉시 사용할 수 있습니다. "
            + "이 단계에서는 파일 자체만 생성하며, 특정 게시글과의 실제 연결 관계는 post 도메인 구현 시점에 post_files 매핑으로 관리됩니다."
    )
    @PostMapping(value = "/images/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadPostImage(
        @Parameter(
            description = "게시글 본문에 삽입할 이미지 파일. JPG, PNG, GIF, WEBP 형식을 허용합니다."
        )
        @RequestPart("file") MultipartFile file
    ) {
        log.debug("POST /api/v1/files/images/posts - 게시글 이미지 업로드 요청");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(imageUploadService.uploadPostImage(file));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "사이트 콘텐츠 이미지 업로드",
        description = "연혁, 기관 소개처럼 공개 사이트 콘텐츠에서 사용할 이미지를 업로드합니다. "
            + "업로드된 이미지는 site-contents 디렉터리에 저장되고 files 테이블에 메타데이터가 기록됩니다. "
            + "응답의 url은 site-content 연혁 photos[].src 등에 그대로 저장해 사용할 수 있습니다."
    )
    @PostMapping(value = "/images/site-contents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadSiteContentImage(
        @Parameter(
            description = "사이트 콘텐츠에서 사용할 이미지 파일. JPG, PNG, GIF, WEBP 형식을 허용합니다."
        )
        @RequestPart("file") MultipartFile file
    ) {
        log.debug("POST /api/v1/files/images/site-contents - 사이트 콘텐츠 이미지 업로드 요청");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(imageUploadService.uploadSiteContentImage(file));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "구매 대상 이미지 업로드",
        description = "구매 요청 작성 과정에서 각 구매 대상 품목의 참고 사진이나 증빙 이미지를 업로드할 때 사용합니다. "
            + "예를 들어 학습 교구, 재료, 비품의 상태나 모델 정보를 보여주는 이미지를 첨부하는 경우를 대상으로 합니다. "
            + "업로드된 이미지는 documents/purchase-items 디렉터리에 저장되고 files 테이블에 메타데이터가 기록됩니다. "
            + "현재 단계에서는 구매 품목과의 직접 매핑 엔티티는 없으며, 후속 request/post 도메인 구현 시 연결될 예정입니다."
    )
    @PostMapping(value = "/images/purchase-items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadPurchaseItemImage(
        @Parameter(
            description = "구매 대상 설명 또는 증빙용 이미지 파일. JPG, PNG, GIF, WEBP 형식을 허용합니다."
        )
        @RequestPart("file") MultipartFile file
    ) {
        log.debug("POST /api/v1/files/images/purchase-items - 구매 대상 이미지 업로드 요청");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(imageUploadService.uploadPurchaseItemImage(file));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "첨부파일 업로드",
        description = "게시글, 요청서, 보고서 등에 연결할 문서형 첨부파일을 업로드합니다. "
            + "주요 사용 사례는 PDF, 엑셀, 워드, 텍스트, 발표자료 등 다운로드가 필요한 문서를 등록하는 상황입니다. "
            + "업로드된 파일은 documents/attachments 디렉터리에 저장되고 files 테이블에 메타데이터가 기록됩니다. "
            + "이 API는 파일 생성만 담당하며, 게시글 첨부 순서나 공개 범위 같은 비즈니스 정보는 "
            + "후속 구현에서 post_attachments 같은 매핑 테이블로 관리됩니다."
    )
    @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadAttachment(
        @Parameter(
            description = "첨부할 문서 파일. PDF, XLS, XLSX, DOC, DOCX, TXT, PPT, PPTX 형식을 허용합니다."
        )
        @RequestPart("file") MultipartFile file
    ) {
        log.debug("POST /api/v1/files/attachments - 첨부파일 업로드 요청");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(attachmentUploadService.uploadAttachment(file));
    }

    @PreAuthorize("hasAnyRole('VOLUNTEER', 'MANAGER', 'ADMIN')")
    @Operation(
        summary = "Google Drive 파일 등록",
        description = "프론트가 Google Drive API로 직접 업로드한 파일의 URL과 메타데이터만 files 테이블에 기록합니다. "
            + "백엔드는 Drive 클라이언트를 사용하지 않고, 응답의 fileId는 기존 게시글 첨부 연결 API에서 그대로 사용할 수 있습니다."
    )
    @PostMapping("/drive")
    public ResponseEntity<FileUploadResponse> registerDriveFile(
        @Valid @RequestBody RegisterDriveFileRequest request
    ) {
        log.debug("POST /api/v1/files/drive - Google Drive 파일 메타데이터 등록 요청");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(driveFileService.registerDriveFile(request));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "첨부파일 다운로드 URL 조회",
        description = "업로드된 첨부파일에 대해 다운로드 가능한 임시 URL을 발급합니다. "
            + "주요 사용 사례는 첨부파일 목록 화면에서 사용자가 문서를 다운로드하려고 할 때입니다. "
            + "GCS 파일은 스토리지 signed URL을 생성하고, Google Drive 파일은 등록된 공유 URL을 그대로 반환합니다."
    )
    @GetMapping("/attachments/{fileId}/download-url")
    public ResponseEntity<FileDownloadUrlResponse> getAttachmentDownloadUrl(
        @Parameter(
            description = "다운로드 URL을 발급할 대상 파일 ID. files 테이블의 UUID 기본 키입니다.",
            example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @PathVariable UUID fileId
    ) {
        log.debug("GET /api/v1/files/attachments/{}/download-url - 첨부파일 다운로드 URL 조회 요청", fileId);
        return ResponseEntity.ok(
            new FileDownloadUrlResponse(attachmentUploadService.getDownloadUrl(fileId))
        );
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "첨부파일 삭제",
        description = "업로드된 첨부파일을 삭제합니다. "
            + "GCS 파일은 스토리지의 실제 객체를 제거하고, Google Drive 파일은 외부 파일 삭제 없이 "
            + "files 테이블의 메타데이터만 soft delete 처리합니다. "
            + "게시글이나 요청서와의 연결 정보가 추후 별도 매핑 테이블로 관리될 경우, "
            + "이 API는 해당 참조 정리 로직과 함께 확장될 수 있습니다."
    )
    @DeleteMapping("/attachments/{fileId}")
    public ResponseEntity<Void> deleteAttachment(
        @Parameter(
            description = "삭제할 파일 ID. files 테이블의 UUID 기본 키입니다.",
            example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @PathVariable UUID fileId
    ) {
        log.debug("DELETE /api/v1/files/attachments/{} - 첨부파일 삭제 요청", fileId);
        attachmentUploadService.deleteAttachment(fileId);
        return ResponseEntity.noContent().build();
    }
}
