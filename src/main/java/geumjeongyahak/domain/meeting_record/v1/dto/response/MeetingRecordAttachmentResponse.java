package geumjeongyahak.domain.meeting_record.v1.dto.response;

import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.meeting_record.entity.MeetingRecordAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "교학 회의록 첨부파일 정보입니다.")
public record MeetingRecordAttachmentResponse(
    @Schema(description = "파일 UUID입니다.", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID fileId,

    @Schema(description = "원본 파일명입니다.", example = "회의자료.pdf")
    String originalName,

    @Schema(description = "MIME 타입입니다.", example = "application/pdf")
    String contentType,

    @Schema(description = "파일 크기(byte)입니다.", example = "204800")
    Long fileSize,

    @Schema(description = "파일 확장자입니다.", example = "pdf")
    String ext,

    @Schema(description = "Google Drive에 저장된 외부 파일이면 true입니다.", example = "false")
    boolean isGoogleDrive,

    @Schema(description = "다운로드 가능한 URL입니다.", example = "https://storage.googleapis.com/...")
    String downloadUrl,

    @Schema(description = "첨부파일 정렬 순서입니다.", example = "0")
    int sortOrder
) {
    public static MeetingRecordAttachmentResponse from(MeetingRecordAttachment attachment) {
        File file = attachment.getFile();
        return new MeetingRecordAttachmentResponse(
            file.getId(),
            file.getOriginalName(),
            file.getContentType(),
            file.getFileSize(),
            file.getExt(),
            file.isGoogleDrive(),
            file.getPublicUrl(),
            attachment.getSortOrder()
        );
    }
}
