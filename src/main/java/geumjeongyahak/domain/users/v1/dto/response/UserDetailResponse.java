package geumjeongyahak.domain.users.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import geumjeongyahak.domain.auth.v1.dto.response.PermissionResponse;
import geumjeongyahak.domain.users.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "사용자 상세 정보")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDetailResponse(
    @Schema(description = "사용자 식별자", example = "1")
    Long id,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "닉네임", example = "까치")
    String nickname,

    @Schema(description = "이메일", example = "user@example.com")
    String email,

    @Schema(description = "전화번호", example = "010-1234-5678")
    String phoneNumber,

    @Schema(description = "역할", examples = { "ADMIN", "MANAGER", "VOLUNTEER", "GUEST" })
    String role,

    @Schema(description = "부서 ID", example = "2")
    Long departmentId,

    @Schema(description = "사용자 권한 목록")
    List<PermissionResponse> permissions,

    @Schema(description = "생성 일시", example = "2024-01-01T12:00:00")
    LocalDateTime createdAt,

    @Schema(description = "수정 일시", example = "2024-01-02T15:30:00")
    LocalDateTime updatedAt
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
            user.getId(),
            user.getName(),
            user.getNickname(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getRole().name(),
            user.getDepartment() != null ? user.getDepartment().getId() : null,
            user.getPermissions().stream()
                .map(permission -> new PermissionResponse(
                    permission.toAuthorityCode(),
                    permission.toAuthorityCode()
                ))
                .toList(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
