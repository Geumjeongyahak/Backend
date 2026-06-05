package geumjeongyahak.domain.department.service;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.v1.dto.response.PermissionResponse;
import geumjeongyahak.domain.base.model.PermissionCode;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.entity.DepartmentPermission;
import geumjeongyahak.domain.department.enums.DepartmentRoleType;
import geumjeongyahak.domain.department.repository.DepartmentPermissionRepository;
import geumjeongyahak.domain.department.v1.dto.request.DepartmentPermissionRequest;
import geumjeongyahak.domain.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentPermissionService {

    private final DepartmentPermissionRepository departmentPermissionRepository;

    public List<DepartmentPermission> getAllPermissions(Long departmentId) {
        return departmentPermissionRepository.findAllByDepartmentId(departmentId);
    }

    public List<DepartmentPermission> getEffectivePermissions(User user) {
        List<DepartmentRoleType> roleTypes = resolveDepartmentRoleTypes(user.getRole());
        if (user.getDepartment() == null || roleTypes.isEmpty()) {
            return List.of();
        }
        return departmentPermissionRepository.findAllByDepartmentIdAndRoleTypeIn(
            user.getDepartment().getId(),
            roleTypes
        );
    }

    public List<PermissionResponse> getAllPermissionResponses(Long departmentId) {
        return getAllPermissions(departmentId).stream()
            .sorted(Comparator
                .comparing(DepartmentPermission::getRoleType)
                .thenComparing(DepartmentPermission::getPermissionCode))
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void replacePermissions(Department department, List<DepartmentPermissionRequest> requests) {
        departmentPermissionRepository.deleteAllByDepartmentId(department.getId());
        if (requests == null) {
            return;
        }
        requests.forEach(request -> addPermission(department, request.roleTypeOrDefault(), request.permissionCode()));
    }

    @Transactional
    public List<PermissionResponse> addMemberPermission(Department department, String permissionCode) {
        addPermission(department, DepartmentRoleType.MEMBER, permissionCode);
        return getAllPermissionResponses(department.getId());
    }

    @Transactional
    public List<PermissionResponse> addPermission(
        Department department, DepartmentRoleType roleType, String permissionCode
    ) {
        String validatedPermissionCode = new PermissionCode(permissionCode).value();
        departmentPermissionRepository.findByDepartmentIdAndRoleTypeAndPermissionCode(
                department.getId(), roleType, validatedPermissionCode)
            .orElseGet(() -> departmentPermissionRepository.save(
                new DepartmentPermission(department, roleType, validatedPermissionCode)));
        return getAllPermissionResponses(department.getId());
    }

    public List<DepartmentRoleType> resolveDepartmentRoleTypes(RoleType roleType) {
        List<DepartmentRoleType> roleTypes = new ArrayList<>();
        if (roleType == RoleType.VOLUNTEER || roleType == RoleType.MANAGER) {
            roleTypes.add(DepartmentRoleType.MEMBER);
        }
        if (roleType == RoleType.MANAGER) {
            roleTypes.add(DepartmentRoleType.MANAGER);
        }
        return roleTypes;
    }

    private PermissionResponse toResponse(DepartmentPermission permission) {
        return PermissionResponse.from(
            permission.getId(),
            permission.toAuthorityCode(),
            permission.getRoleType().name()
        );
    }
}
