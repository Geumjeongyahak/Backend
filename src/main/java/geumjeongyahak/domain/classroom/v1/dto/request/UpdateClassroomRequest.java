package geumjeongyahak.domain.classroom.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidClassroomType;

@Schema(description = "반 수정 요청")
public record UpdateClassroomRequest(

        @Schema(description = "반 이름", example = "해바라기반")
        @Size(min=2, max=50, message = "반 이름은 2자 이상 50자 이하로 입력해주세요.")
        String name,

        @Schema(description = "반 유형", examples = {"WEEKDAY", "WEEKEND"})
        @ValidClassroomType
        String type,

        @Schema(description = "반 설명", example = "초등 수준의 기초 교육을 제공하는 반")
        String description
) {

}
