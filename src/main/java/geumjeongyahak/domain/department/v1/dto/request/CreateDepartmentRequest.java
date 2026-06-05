package geumjeongyahak.domain.department.v1.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "부서 생성 요청. permissions는 부서 직책별 권한 목록이며 roleType을 생략하면 MEMBER로 저장됩니다.")
public record CreateDepartmentRequest(
        @Schema(description = "부서 이름", example = "개발팀")
        @NotBlank(message = "부서 이름은 필수입니다.")
        String name,

        @Schema(description = "부서 설명", example = "소프트웨어 개발을 담당하는 팀")
        @NotBlank(message = "부서 설명은 필수입니다.")
        String description,

        @Schema(description = "부서에 부여할 직책별 권한 목록. roleType이 MEMBER인 권한은 일반 부서원에게, MANAGER인 권한은 부서장에게 적용됩니다.", nullable = true)
        List<@Valid DepartmentPermissionRequest> permissions
) {
}
