package sonmoeum.api.v1.classrooms.dto.request;

import sonmoeum.domain.classroom.enums.ClassroomType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateClassroomRequest(
    @Schema(description = "분반 이름", example = "초등부 1반")
    @Size(max = 50, message = "분반 이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "분반 타입", example = "WEEKDAY")
    ClassroomType type,

    @Schema(description = "분반 설명", example = "초등학교 저학년 대상 평일반")
    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    String description
) {}

