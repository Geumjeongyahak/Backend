package geumjeongyahak.domain.department.v1.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "부서 생성 요청")
public record CreateDepartmentRequest(
        @Schema(description = "부서 이름", example = "개발팀")
        @NotBlank(message = "부서 이름은 필수입니다.")
        String name,

        @Schema(description = "부서 설명", example = "소프트웨어 개발을 담당하는 팀")
        @NotBlank(message = "부서 설명은 필수입니다.")
        String description
) {
}
