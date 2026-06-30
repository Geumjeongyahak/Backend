package geumjeongyahak.domain.users.service.dto;

import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import java.time.LocalDateTime;
import java.util.List;

public record AdminUserRow(
    Long id,
    String name,
    String email,
    String phoneNumber,
    String role,
    Long departmentId,
    String departmentName,
    Long classroomId,
    String classroomName,
    List<String> permissions,
    LocalDateTime createdAt
) {
    public static AdminUserRow from(User user) {
        return new AdminUserRow(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getRole().name(),
            user.getDepartment() != null ? user.getDepartment().getId() : null,
            user.getDepartment() != null ? user.getDepartment().getName() : "-",
            user.getClassroom() != null ? user.getClassroom().getId() : null,
            user.getClassroom() != null ? user.getClassroom().getName() : "-",
            user.getPermissions().stream()
                .map(UserPermission::toAuthorityCode)
                .sorted()
                .toList(),
            user.getCreatedAt()
        );
    }
}
