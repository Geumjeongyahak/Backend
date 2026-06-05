package geumjeongyahak.domain.department.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

@Schema(description = "부서 수정 요청. permissions가 null이면 기존 권한을 유지하고, 값이 있으면 기존 권한을 전체 교체합니다.")
public record UpdateDepartmentRequest(
        @Schema(description = "부서 이름", example = "개발팀")
        String name,
        @Schema(description = "부서 설명", example = "소프트웨어 개발을 담당하는 팀")
        String description,
        @Schema(description = "부서 직책별 권한 목록. null이면 유지, 빈 배열이면 전체 삭제, 값이 있으면 전체 교체합니다.", nullable = true)
        List<@Valid DepartmentPermissionRequest> permissions
) {

}
