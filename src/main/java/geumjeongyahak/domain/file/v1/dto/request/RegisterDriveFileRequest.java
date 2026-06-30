package geumjeongyahak.domain.file.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Google Drive에 업로드된 파일의 메타데이터 등록 요청입니다.")
public record RegisterDriveFileRequest(
    @NotBlank
    @Size(max = 1000)
    @Schema(description = "Google Drive 공유 URL입니다.", example = "https://drive.google.com/file/d/abc123/view?usp=sharing")
    String driveUrl,

    @NotBlank
    @Size(max = 255)
    @Schema(description = "화면에 표시할 파일명입니다.", example = "2026 자료집.pdf")
    String originalName,

    @Size(max = 100)
    @Schema(description = "파일 MIME 타입입니다. 없으면 application/octet-stream으로 저장합니다.", example = "application/pdf")
    String mimeType,

    @PositiveOrZero
    @Schema(description = "파일 크기(byte)입니다. Drive에서 알 수 없으면 생략할 수 있습니다.", example = "204800")
    Long fileSize
) {
}
