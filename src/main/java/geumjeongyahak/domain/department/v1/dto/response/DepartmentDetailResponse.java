package geumjeongyahak.domain.department.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.auth.v1.dto.response.PermissionResponse;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.entity.DepartmentPermission;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.v1.dto.response.UserSimpleResponse;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Schema(description = "부서 상세 응답. 부서 정보, 직책별 권한 목록, 소속 사용자 목록을 포함합니다.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DepartmentDetailResponse (
        @Schema(description = "부서 ID", example = "1")
        Long id,

        @Schema(description = "부서 이름", example = "개발팀")
        String name,

        @Schema(description = "부서 설명", example = "소프트웨어 개발을 담당하는 팀")
        String description,

        @Schema(description = "부서 직책별 권한 목록. source가 MEMBER이면 일반 부서원 권한, MANAGER이면 부서장 추가 권한입니다.")
        List<PermissionResponse> permissions,

        @Schema(description = "부서에 속한 사용자 목록")
        List<UserSimpleResponse> users,

        @Schema(description = "생성 일시", example = "2024-01-01T12:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시", example = "2024-01-02T15:30:00")
        LocalDateTime updatedAt
) {
        public static DepartmentDetailResponse from(
                Department department,
                Collection<User> users,
                Collection<DepartmentPermission> permissions
        ) {
            return new DepartmentDetailResponse(
                    department.getId(),
                    department.getName(),
                    department.getDescription(),
                    permissions.stream()
                            .map(permission -> {
                                    String authorityCode = permission.toAuthorityCode();
                                    return new PermissionResponse(
                                            authorityCode,
                                            authorityCode,
                                            permission.getRoleType().name()
                                    );
                            })
                            .toList(),
                    users.stream()
                            .map(UserSimpleResponse::from)
                            .toList(),
                    department.getCreatedAt(),
                    department.getUpdatedAt()
            );
        }
}
