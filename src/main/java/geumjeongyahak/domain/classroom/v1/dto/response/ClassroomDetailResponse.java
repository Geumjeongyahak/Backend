package geumjeongyahak.domain.classroom.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.classroom.entity.Classroom;

import java.time.LocalDateTime;

@Schema(description = "분반 상세 응답 DTO")
public record ClassroomDetailResponse(
        @Schema(description = "분반 ID", example = "1")
        Long id,

        @Schema(description = "분반 이름", example = "해바라기반")
        String name,

        @Schema(description = "분반 유형. WEEKDAY는 주중반, WEEKEND는 주말반입니다.", example = "WEEKDAY")
        String type,

        @Schema(description = "분반 설명", example = "초등 수준의 기초 교육을 제공하는 분반")
        String description,

        @Schema(description = "생성 일시", example = "2024-01-01T12:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시", example = "2024-01-02T12:00:00")
        LocalDateTime updatedAt
) {

    public static ClassroomDetailResponse from(Classroom classroom) {
        return new ClassroomDetailResponse(
                classroom.getId(),
                classroom.getName(),
                classroom.getType().name(),
                classroom.getDescription(),
                classroom.getCreatedAt(),
                classroom.getUpdatedAt()
        );
    }
}
