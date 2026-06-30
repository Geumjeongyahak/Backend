package geumjeongyahak.domain.subject.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "과목 기본 정보 수정 요청")
public class UpdateSubjectBasicRequest {

    @Size(max = 50)
    @Pattern(regexp = ".*\\S.*", message = "name은 공백일 수 없습니다.")
    @Schema(description = "과목명", example = "수학")
    private String name;

    @Schema(description = "과목 설명", example = "과목 설명 수정")
    private String description;

    public UpdateSubjectBasicRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
