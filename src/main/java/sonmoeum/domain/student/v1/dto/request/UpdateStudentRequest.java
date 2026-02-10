package sonmoeum.domain.student.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import sonmoeum.common.validation.annotation.ValidPhoneNumber;

public record UpdateStudentRequest(
    @Schema(description = "이름", example = "김철수")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "전화번호", example = "010-4321-5678")
    @ValidPhoneNumber
    String phoneNumber,

    @Schema(description = "설명", example = "학생")
    String description,

    @Schema(
        description = "상태",
        example = "ON_LEAVE",
        allowableValues = { "ENROLLED", "ON_LEAVE", "COMPLETED" }
    )
    String status
) {}
