package sonmoeum.api.v1.subjects.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateSubjectRequest(
    @Schema(description = "과목 이름", example = "국어 심화")
    @Size(max = 50, message = "과목 이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "선생님 ID", example = "2")
    Long teacherId,

    @Schema(description = "설명", example = "2학기 국어 수업 변경")
    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    String description
) {}
