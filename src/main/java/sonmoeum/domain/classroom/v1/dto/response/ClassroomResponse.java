package sonmoeum.domain.classroom.v1.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import sonmoeum.domain.classroom.entity.Classroom;

@Schema(description = "교실 응답 DTO")
public record ClassroomResponse(
        @Schema(description = "교실 ID", example = "1")
        Long id,

        @Schema(description = "교실 이름", example = "해바라기 반")
        String name,

        @Schema(description = "교실 유형", example = "WEEKDAY")
        String type
) {
    public static ClassroomResponse from(Classroom classroom) {
        return new ClassroomResponse(
                classroom.getId(),
                classroom.getName(),
                classroom.getType().name()
        );
    }

}
