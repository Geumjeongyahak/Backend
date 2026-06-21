package geumjeongyahak.domain.users.service;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.domain.users.v1.dto.request.UpdateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;
import geumjeongyahak.domain.users.service.dto.AdminUserRow;
import geumjeongyahak.domain.users.service.dto.ClassroomOption;
import geumjeongyahak.domain.users.service.dto.DepartmentOption;
import geumjeongyahak.domain.users.service.dto.UserFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final ClassroomProxyService classroomProxyService;
    private final UserCrudService userCrudService;
    private final UserPermissionService userPermissionService;

    public AdminPage<AdminUserRow> getUsers(UserFilter filter) {
        List<AdminUserRow> rows = userRepository.findAllByIsDeletedFalse(
                Sort.by(Sort.Direction.DESC, "createdAt")
            )
            .stream()
            .filter(user -> matchesKeyword(user, filter.keyword()))
            .filter(user -> isBlank(filter.role()) || user.getRole().name().equals(filter.role()))
            .filter(user -> filter.departmentId() == null
                || (user.getDepartment() != null && user.getDepartment().getId().equals(filter.departmentId())))
            .filter(user -> filter.classroomId() == null
                || (user.getClassroom() != null && user.getClassroom().getId().equals(filter.classroomId())))
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
            "classroomName", Comparator.comparing(AdminUserRow::classroomName, Comparator.nullsLast(String::compareToIgnoreCase)),
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

    public List<ClassroomOption> getClassroomOptions() {
        return classroomProxyService.getActiveClassroomsOrderByName()
            .stream()
            .map(ClassroomOption::from)
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
        LocalDate birthDate,
        String role,
        Long departmentId,
        Long classroomId
    ) {
        return userCrudService.createUser(new CreateUserRequest(
            email,
            name,
            password,
            phoneNumber,
            birthDate,
            role,
            departmentId,
            classroomId
        )).id();
    }

    @Transactional
    public void updateUser(
        Long userId,
        String email,
        String name,
        String phoneNumber,
        LocalDate birthDate,
        String role,
        Long departmentId,
        Long classroomId
    ) {
        userCrudService.updateUser(userId, new UpdateUserRequest(
            name,
            phoneNumber,
            birthDate,
            email,
            null,
            role,
            departmentId,
            classroomId
        ));
    }

    @Transactional
    public void updateRole(Long userId, String role) {
        userCrudService.updateUser(userId, new UpdateUserRequest(
            null,
            null,
            null,
            null,
            null,
            role,
            null,
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
            null,
            departmentId,
            null
        ));
    }

    @Transactional
    public void updateClassroom(Long userId, Long classroomId) {
        userCrudService.updateUser(userId, new UpdateUserRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            classroomId
        ));
    }

    @Transactional
    public void addPermission(Long userId, String permissionCode) {
        userPermissionService.addPermission(userId, permissionCode);
    }
}
