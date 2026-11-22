package geumjeongyahak.domain.subject.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "시간표 기간 복사 요청. 보낸 과목만 새 기간으로 복사한다")
public record SubjectCopyRequest(
    @Schema(description = "복사할 원본 과목 ID 목록 (최대 200개, 중복 불가)", example = "[57, 58, 59]")
    @NotEmpty(message = "복사할 과목을 1개 이상 보내야 합니다.")
    @Size(max = 200, message = "한 번에 200개까지 복사할 수 있습니다.")
    List<@NotNull Long> subjectIds,

    @Schema(description = "새 기간 시작일", example = "2026-11-01")
    @NotNull(message = "startAt은 필수입니다.")
    LocalDate startAt,

    @Schema(description = "새 기간 종료일", example = "2026-11-30")
    @NotNull(message = "endAt은 필수입니다.")
    LocalDate endAt
) {
}
