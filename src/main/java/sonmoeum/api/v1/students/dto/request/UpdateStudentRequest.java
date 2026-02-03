package sonmoeum.api.v1.students.dto.request;

import sonmoeum.common.validation.annotation.ValidPhoneNumber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(
    @Schema(description = "학생 이름", example = "김학생")
    @Size(max = 50, message = "학생 이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "학생 전화번호", example = "010-1111-2222")
    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
    @ValidPhoneNumber
    String phoneNumber,

    @Schema(description = "학생 설명", example = "특이사항 없음")
    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    String description
) {}
