package geumjeongyahak.domain.file.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "파일 다운로드 URL 응답. "
        + "스토리지에서 발급한 임시 접근 링크를 담고 있으며, "
        + "직접 파일을 다운로드하거나 브라우저 새 창으로 열 때 사용할 수 있습니다."
)
public record FileDownloadUrlResponse(
    @Schema(description = "다운로드용 임시 URL입니다. 일반적으로 만료 시간이 있으며, 서버가 아닌 스토리지로 직접 접근하게 됩니다.", example = "https://storage.googleapis.com/example-bucket/documents/attachments/example.pdf")
    String downloadUrl
) {
}
