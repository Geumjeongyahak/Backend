package geumjeongyahak.domain.users.v1.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import geumjeongyahak.domain.auth.v1.dto.response.RoleResponse;
import geumjeongyahak.domain.users.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
    @Schema(description = "사용자 식별자", example = "1")
    Long id,

    @Schema(description = "사용자 아이디", example = "user1324")
    String username,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "이메일", example = "user@example.com")
    String email,

    @Schema(description = "전화번호", example = "010-1234-5678")
    String phoneNumber,

    @Schema(description = "사용자 권한(역할, 부서권한, 부가권한 등)", example = "ADMIN")
    List<RoleResponse> roles
) {
    
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getRoles().stream()
                    .map(userRole -> RoleResponse.from(userRole.getRoleType()))
                    .toList()

        );
    }
}
