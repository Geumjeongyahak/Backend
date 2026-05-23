package geumjeongyahak.domain.student.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidPhoneNumber;
import geumjeongyahak.domain.student.enums.StudentStatus;
import java.util.List;

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
    StudentStatus status,

    @Schema(description = "변경할 소속 분반 식별자 목록", example = "[1, 2]")
    @Size(min = 1, message = "분반 ID 목록은 1개 이상이어야 합니다.")
    List<@NotNull(message = "분반 ID는 필수입니다.") Long> classroomIds
) {}
