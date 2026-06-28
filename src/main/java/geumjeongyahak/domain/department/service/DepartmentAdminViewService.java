package geumjeongyahak.domain.department.service;

import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.entity.DepartmentPermission;
import geumjeongyahak.domain.department.enums.DepartmentRoleType;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.department.v1.dto.request.CreateDepartmentRequest;
import geumjeongyahak.domain.department.v1.dto.request.UpdateDepartmentRequest;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentDetailResponse;
import geumjeongyahak.domain.users.service.UserProxyService;
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
public class DepartmentAdminViewService {

    private final DepartmentRepository departmentRepository;
    private final UserProxyService userProxyService;
    private final DepartmentCrudService departmentCrudService;
    private final DepartmentPermissionService departmentPermissionService;

    public List<AdminDepartmentRow> getDepartments(DepartmentFilter filter) {
        List<AdminDepartmentRow> rows = departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
            .stream()
            .filter(department -> matchesKeyword(department, filter.keyword()))
            .filter(department -> isBlank(filter.permissionCode())
                || departmentPermissionService.getAllPermissions(department.getId()).stream()
                    .anyMatch(permission -> permission.getPermissionCode().contains(filter.permissionCode().trim())))
            .map(department -> AdminDepartmentRow.from(
                department,
                userProxyService.countActiveUsersByDepartmentId(department.getId()),
                departmentPermissionService.getAllPermissions(department.getId())
            ))
            .filter(row -> filter.minMemberCount() == null || row.memberCount() >= filter.minMemberCount())
            .toList();

        return AdminSorts.sort(rows, filter.sort(), Map.of(
            "id", Comparator.comparing(AdminDepartmentRow::id),
            "name", Comparator.comparing(AdminDepartmentRow::name, Comparator.nullsLast(String::compareToIgnoreCase)),
            "memberCount", Comparator.comparing(AdminDepartmentRow::memberCount),
            "createdAt", Comparator.comparing(AdminDepartmentRow::createdAt, Comparator.nullsLast(LocalDateTime::compareTo))
        ), "name,ASC");
    }

    private boolean matchesKeyword(Department department, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(department.getName(), normalized)
            || contains(department.getDescription(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Transactional
    public Long createDepartment(String name, String description) {
        return departmentCrudService.createDepartment(new CreateDepartmentRequest(name, description, List.of())).id();
    }

    public DepartmentDetailResponse getDepartment(Long departmentId) {
        return departmentCrudService.getDepartmentDetailById(departmentId);
    }

    @Transactional
    public void updateDepartment(Long departmentId, String name, String description) {
        departmentCrudService.updateDepartment(departmentId, new UpdateDepartmentRequest(name, description, null));
    }

    @Transactional
    public void addPermission(Long departmentId, DepartmentRoleType roleType, String permissionCode) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                CommonErrorCode.RESOURCE_NOT_FOUND,
                "부서를 찾을 수 없습니다 - ID: " + departmentId
            ));
        departmentPermissionService.addPermission(
            department,
            roleType != null ? roleType : DepartmentRoleType.MEMBER,
            permissionCode
        );
    }

    public record AdminDepartmentRow(
        Long id,
        String name,
        String description,
        long memberCount,
        List<String> permissions,
        LocalDateTime createdAt
    ) {
        private static AdminDepartmentRow from(
            Department department, long memberCount, List<DepartmentPermission> permissions
        ) {
            return new AdminDepartmentRow(
                department.getId(),
                department.getName(),
                department.getDescription(),
                memberCount,
                permissions.stream()
                    .map(permission -> permission.getRoleType().name() + ":" + permission.toAuthorityCode())
                    .sorted()
                    .toList(),
                department.getCreatedAt()
            );
        }
    }

    public record DepartmentFilter(
        String keyword,
        String permissionCode,
        Long minMemberCount,
        String sort
    ) {
    }
}
