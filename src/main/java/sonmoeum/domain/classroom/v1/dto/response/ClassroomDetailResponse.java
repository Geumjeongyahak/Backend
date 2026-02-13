package sonmoeum.domain.classroom.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import sonmoeum.domain.classroom.entity.Classroom;

import java.time.LocalDateTime;

@Schema(description = "교실 응답 DTO")
public record ClassroomDetailResponse(
        @Schema(description = "교실 ID", example = "1")
        Long id,

        @Schema(description = "교실 이름", example = "해바라기 반")
        String name,

        @Schema(description = "교실 유형", example = "WEEKDAY")
        String type,

        @Schema(description = "교실 설명", example = "이 교실은 수학 수업을 위한 교실입니다.")
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
