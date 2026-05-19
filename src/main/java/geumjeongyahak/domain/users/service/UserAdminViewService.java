package geumjeongyahak.domain.users.service;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.domain.users.v1.dto.request.UpdateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminViewService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final UserCrudService userCrudService;
    private final UserPermissionService userPermissionService;

    public AdminPage<AdminUserRow> getUsers(UserFilter filter) {
        List<AdminUserRow> rows = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream()
            .filter(user -> matchesKeyword(user, filter.keyword()))
            .filter(user -> isBlank(filter.role()) || user.getRole().name().equals(filter.role()))
            .filter(user -> filter.departmentId() == null
                || (user.getDepartment() != null && user.getDepartment().getId().equals(filter.departmentId())))
            .filter(user -> isBlank(filter.permissionCode())
                || user.getPermissions().stream()
                    .anyMatch(permission -> permission.getPermissionCode().contains(filter.permissionCode().trim())))
            .map(AdminUserRow::from)
            .toList();

        return AdminPage.from(sortUsers(rows, filter.sort()), filter.page(), filter.size());
    }

    private List<AdminUserRow> sortUsers(List<AdminUserRow> rows, String sort) {
        return AdminSorts.sort(rows, sort, Map.of(
            "id", Comparator.comparing(AdminUserRow::id),
            "name", Comparator.comparing(AdminUserRow::name, Comparator.nullsLast(String::compareToIgnoreCase)),
            "email", Comparator.comparing(AdminUserRow::email, Comparator.nullsLast(String::compareToIgnoreCase)),
            "role", Comparator.comparing(AdminUserRow::role, Comparator.nullsLast(String::compareToIgnoreCase)),
            "departmentName", Comparator.comparing(AdminUserRow::departmentName, Comparator.nullsLast(String::compareToIgnoreCase)),
            "createdAt", Comparator.comparing(AdminUserRow::createdAt, Comparator.nullsLast(LocalDateTime::compareTo))
        ), "createdAt,DESC");
    }

    private boolean matchesKeyword(User user, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(user.getName(), normalized)
            || contains(user.getEmail(), normalized)
            || contains(user.getPhoneNumber(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public List<DepartmentOption> getDepartmentOptions() {
        return departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
            .stream()
            .map(DepartmentOption::from)
            .toList();
    }

    public UserDetailResponse getUser(Long userId) {
        return userCrudService.getUserById(userId);
    }

    @Transactional
    public Long createUser(
        String email,
        String name,
        String password,
        String phoneNumber,
        String role,
        Long departmentId
    ) {
        return userCrudService.createUser(new CreateUserRequest(
            email,
            name,
            password,
            phoneNumber,
            role,
            departmentId
        )).id();
    }

    @Transactional
    public void updateUser(
        Long userId,
        String email,
        String name,
        String phoneNumber,
        String role,
        Long departmentId
    ) {
        userCrudService.updateUser(userId, new UpdateUserRequest(
            name,
            phoneNumber,
            email,
            null,
            role,
            departmentId
        ));
    }

    @Transactional
    public void updateRole(Long userId, String role) {
        userCrudService.updateUser(userId, new UpdateUserRequest(
            null,
            null,
            null,
            null,
            role,
            null
        ));
    }

    @Transactional
    public void updateDepartment(Long userId, Long departmentId) {
        userCrudService.updateUser(userId, new UpdateUserRequest(
            null,
            null,
            null,
            null,
            null,
            departmentId
        ));
    }

    @Transactional
    public void addPermission(Long userId, String permissionCode) {
        userPermissionService.addPermission(userId, permissionCode);
    }

    public record DepartmentOption(
        Long id,
        String name
    ) {
        private static DepartmentOption from(Department department) {
            return new DepartmentOption(department.getId(), department.getName());
        }
    }

    public record UserFilter(
        String keyword,
        String role,
        Long departmentId,
        String permissionCode,
        Integer page,
        Integer size,
        String sort
    ) {
    }

    public record AdminUserRow(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String role,
        Long departmentId,
        String departmentName,
        List<String> permissions,
        LocalDateTime createdAt
    ) {
        private static AdminUserRow from(User user) {
            return new AdminUserRow(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getDepartment() != null ? user.getDepartment().getName() : "-",
                user.getPermissions().stream()
                    .map(UserPermission::toAuthorityCode)
                    .sorted()
                    .toList(),
                user.getCreatedAt()
            );
        }
    }
}
