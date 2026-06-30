package geumjeongyahak.domain.file.v1.dto.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.file.entity.File;

@Schema(
    description = "파일 업로드 응답. "
        + "스토리지 업로드가 완료된 뒤, "
        + "클라이언트가 즉시 사용할 수 있는 접근 URL과 기본 파일 정보만 반환합니다."
)
public record FileUploadResponse(
    @Schema(description = "저장된 파일의 UUID 식별자입니다. 후속 매핑(post_files, post_attachments 등)에서 참조할 수 있습니다.", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID fileId,

    @Schema(description = "사용자가 업로드한 원본 파일명입니다. 프로필 이미지처럼 서버에서 변환하는 경우 변환 후 파일명이 반환될 수 있습니다.", example = "example.png")
    String originalName,

    @Schema(description = "파일의 MIME 타입입니다. 클라이언트 렌더링 또는 다운로드 처리에 활용할 수 있습니다.", example = "image/png")
    String contentType,

    @Schema(description = "저장된 파일 크기(byte)입니다. 업로드 후 파일 검증 또는 화면 표시용으로 사용할 수 있습니다.", example = "102400")
    Long fileSize,

    @Schema(description = "파일 확장자입니다. 변환이 일어나는 경우 최종 저장 확장자를 반환합니다.", example = "png")
    String ext,

    @Schema(description = "Google Drive에 저장된 외부 파일이면 true입니다.", example = "false")
    boolean isGoogleDrive,

    @Schema(description = "업로드 직후 클라이언트가 사용할 접근 URL입니다. 이미지라면 즉시 미리보기에 사용할 수 있고, 문서라면 후속 다운로드 처리에 활용할 수 있습니다.", example = "https://storage.googleapis.com/example-bucket/editor/example.png")
    String url
) {

    public static FileUploadResponse from(File file, String url) {
        return new FileUploadResponse(
            file.getId(),
            file.getOriginalName(),
            file.getContentType(),
            file.getFileSize(),
            file.getExt(),
            file.isGoogleDrive(),
            url
        );
    }
}
