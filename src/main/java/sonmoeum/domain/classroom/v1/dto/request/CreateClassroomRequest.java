package sonmoeum.domain.classroom.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sonmoeum.common.validation.annotation.ValidClassroomType;

@Schema(description = "반 생성 요청")
public record CreateClassroomRequest(
        @Schema(description = "반 이름", example = "해바라기반")
        @NotBlank(message = "반 이름은 필수입니다.")
        @Size(min=2, max=50, message = "반 이름은 2자 이상 50자 이하로 입력해주세요.")
        String name,

        @Schema(description = "반 유형", examples = {"WEEKDAY", "WEEKEND"})
        @ValidClassroomType
        @NotNull(message = "반 유형은 필수입니다.")
        String type,

        @Schema(description = "반 설명", example = "초등 수준의 기초 교육을 제공하는 반")
        String description
) {
}
