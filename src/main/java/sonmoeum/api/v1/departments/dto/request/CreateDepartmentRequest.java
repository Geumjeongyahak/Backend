package sonmoeum.api.v1.departments.dto.request;

import java.util.List;

import sonmoeum.common.validation.annotation.ValidPermissions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
    @Schema(description = "부서 이름", example = "교무부")
    @NotBlank(message = "부서 이름은 필수입니다.")
    @Size(max = 100, message = "부서 이름은 100자 이하여야 합니다.")
    String name,

    @Schema(description = "부서 설명", example = "교무 관련 업무 수행")
    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    String description,

    @Schema(description = "부서 권한 목록", example = "[\"MANAGE_LESSONS\", \"MANAGE_STUDENTS\"]")
    @ValidPermissions
    List<String> permissionTypes
) {}
