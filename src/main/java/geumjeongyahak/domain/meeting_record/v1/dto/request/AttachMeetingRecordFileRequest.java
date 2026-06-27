package geumjeongyahak.domain.meeting_record.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "이미 등록된 파일을 교학 회의록 첨부파일로 연결하는 요청입니다.")
public record AttachMeetingRecordFileRequest(
    @NotNull
    @Schema(description = "files 테이블의 파일 UUID입니다.", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID fileId,

    @Schema(description = "첨부파일 정렬 순서입니다. 생략하면 마지막 순서로 추가됩니다.", example = "0")
    Integer sortOrder
) {
}
