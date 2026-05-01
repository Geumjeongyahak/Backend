package geumjeongyahak.domain.department.service;

import geumjeongyahak.domain.auth.v1.dto.response.PermissionResponse;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.entity.DepartmentPermission;
import geumjeongyahak.domain.department.repository.DepartmentPermissionRepository;
import geumjeongyahak.domain.department.v1.dto.request.DepartmentPermissionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentPermissionService {

    private final DepartmentPermissionRepository departmentPermissionRepository;

    public List<PermissionResponse> getAllPermissions(Long departmentId) {
        return departmentPermissionRepository.findAllByDepartmentId(departmentId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void replacePermissions(Department department, List<DepartmentPermissionRequest> requests) {
        department.clearPermissions();
        if (requests == null) {
            return;
        }
        requests.forEach(request -> department.addPermission(request.permissionCode()));
    }

    @Transactional
    public List<PermissionResponse> addPermission(Department department, String permissionCode) {
        departmentPermissionRepository.findByDepartmentIdAndPermissionCode(department.getId(), permissionCode)
            .orElseGet(() -> departmentPermissionRepository.save(new DepartmentPermission(department, permissionCode)));
        return getAllPermissions(department.getId());
    }

    private PermissionResponse toResponse(DepartmentPermission permission) {
        return new PermissionResponse(permission.toAuthorityCode(), permission.toAuthorityCode());
    }
}
