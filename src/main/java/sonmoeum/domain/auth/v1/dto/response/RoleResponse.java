package sonmoeum.domain.auth.v1.dto.response;

import sonmoeum.domain.auth.enums.RoleType;

public record RoleResponse(
        String name,
        Long level,
        Long Code
) {
    public static RoleResponse from(RoleType roleType) {
        return new RoleResponse(
                roleType.name(),
                roleType.getLevel(),
                roleType.getId()
        );
    }
}
