package geumjeongyahak.domain.classroom.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidClassroomType;

@Schema(description = "분반 수정 요청")
public record UpdateClassroomRequest(

        @Schema(description = "분반 이름", example = "해바라기반")
        @Size(min=2, max=50, message = "분반 이름은 2자 이상 50자 이하로 입력해주세요.")
        String name,

        @Schema(description = "분반 유형. WEEKDAY는 주중반, WEEKEND는 주말반입니다.", examples = {"WEEKDAY", "WEEKEND"})
        @ValidClassroomType
        String type,

        @Schema(
                description = "분반 설명. 값을 생략하면 기존 설명을 유지하고, 빈 문자열을 전달하면 설명을 비웁니다.",
                example = "초등 수준의 기초 교육을 제공하는 분반"
        )
        String description
) {

}
