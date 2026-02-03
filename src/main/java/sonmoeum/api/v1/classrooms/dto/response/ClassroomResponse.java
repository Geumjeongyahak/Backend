package sonmoeum.api.v1.classrooms.dto.response;

import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.classroom.enums.ClassroomType;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClassroomResponse(
    @Schema(description = "분반 ID", example = "1")
    Long id,
    @Schema(description = "분반 이름", example = "초등부 1반")
    String name,
    @Schema(description = "분반 타입", example = "WEEKDAY")
    ClassroomType type,
    @Schema(description = "분반 설명", example = "초등학교 저학년 대상 평일반")
    String description
) {
    public static ClassroomResponse from(Classroom classroom) {
        return new ClassroomResponse(
            classroom.getId(),
            classroom.getName(),
            classroom.getType(),
            classroom.getDescription()
        );
    }
}

