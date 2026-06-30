package geumjeongyahak.domain.classroom.v1.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.classroom.entity.Classroom;

@Schema(description = "분반 목록 응답 DTO")
public record ClassroomSummaryResponse(
        @Schema(description = "분반 ID", example = "1")
        Long id,

        @Schema(description = "분반 이름", example = "해바라기반")
        String name,

        @Schema(description = "분반 유형. WEEKDAY는 주중반, WEEKEND는 주말반입니다.", example = "WEEKDAY")
        String type
) {
    public static ClassroomSummaryResponse from(Classroom classroom) {
        return new ClassroomSummaryResponse(
                classroom.getId(),
                classroom.getName(),
                classroom.getType().name()
        );
    }

}
