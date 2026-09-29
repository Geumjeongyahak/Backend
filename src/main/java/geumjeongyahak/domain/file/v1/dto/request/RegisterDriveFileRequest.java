package geumjeongyahak.domain.file.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Google Drive 파일 등록 요청입니다. 이름 · 형식 · 크기는 서버가 Drive에서 직접 조회해 저장하므로 보내도 쓰지 않습니다.")
public record RegisterDriveFileRequest(
    @NotBlank
    @Size(max = 1000)
    @Schema(description = "Google Drive 공유 URL입니다.", example = "https://drive.google.com/file/d/abc123/view?usp=sharing")
    String driveUrl,

    @Size(max = 255)
    @Schema(description = "쓰지 않습니다. 서버가 Drive의 파일명을 저장합니다.", example = "2026 자료집.pdf", deprecated = true)
    String originalName,

    @Size(max = 100)
    @Schema(description = "쓰지 않습니다. 서버가 Drive의 MIME 타입을 저장합니다.", example = "application/pdf", deprecated = true)
    String mimeType,

    @PositiveOrZero
    @Schema(description = "쓰지 않습니다. 서버가 Drive의 파일 크기를 저장합니다.", example = "204800", deprecated = true)
    Long fileSize
) {
}
