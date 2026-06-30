package geumjeongyahak.domain.users.service.dto;

public record UserFilter(
    String keyword,
    String role,
    Long departmentId,
    Long classroomId,
    String permissionCode,
    Integer page,
    Integer size,
    String sort
) {
}
