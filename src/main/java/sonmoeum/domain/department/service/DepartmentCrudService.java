package sonmoeum.domain.department.service;

import java.util.stream.Collectors;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.departments.dto.request.CreateDepartmentRequest;
import sonmoeum.api.v1.departments.dto.request.UpdateDepartmentRequest;
import sonmoeum.api.v1.departments.dto.response.DepartmentResponse;
import sonmoeum.common.event.EventPublisher;
import sonmoeum.domain.auth.enums.PermissionType;
import sonmoeum.domain.auth.entity.DepartmentPermission;
import sonmoeum.domain.department.entity.Department;
import sonmoeum.domain.department.event.DepartmentPermissionsGrantedEvent;
import sonmoeum.domain.department.event.DepartmentPermissionsRevokedEvent;
import sonmoeum.domain.department.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepartmentCrudService {
    private final DepartmentRepository departmentRepository;
    private final EventPublisher eventPublisher;

    public DepartmentResponse getDepartmentById(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 부서가 존재하지 않습니다."));
        return DepartmentResponse.from(department);
    }

    public BasePageResponse<DepartmentResponse> getDepartmentPagination(
        BasePageRequest pageRequest
    ) {
        return BasePageResponse.from(
            departmentRepository.findAll(pageRequest.toPageRequest())
        ).convertTo(DepartmentResponse::from);
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        Department department = new Department(
            request.name(),
            request.permissionTypes() != null ? 
                request.permissionTypes().stream()
                    .map(PermissionType::valueOf)
                    .toList() : null,
            request.description()
        );
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 부서가 존재하지 않습니다."));
        
        if (request.name() != null) {
            department.updateName(request.name());
        }
        if (request.description() != null) {
            department.updateDescription(request.description());
        }
        if (request.permissionTypes() != null) {
            java.util.Set<PermissionType> oldPermissions = department.getPermissions().stream()
                .map(DepartmentPermission::getPermissionType)
                .collect(Collectors.toSet());
            
            java.util.Set<PermissionType> newPermissions = request.permissionTypes().stream()
                .map(PermissionType::valueOf)
                .collect(Collectors.toSet());
                
            java.util.List<PermissionType> addedPermissions = newPermissions.stream()
                .filter(p -> !oldPermissions.contains(p))
                .toList();
                
            java.util.List<PermissionType> removedPermissions = oldPermissions.stream()
                .filter(p -> !newPermissions.contains(p))
                .toList();
                
            if (!addedPermissions.isEmpty()) {
                addedPermissions.forEach(department::addPermission);
                eventPublisher.publish(new DepartmentPermissionsGrantedEvent(departmentId, addedPermissions));
            }
            
            if (!removedPermissions.isEmpty()) {
                removedPermissions.forEach(department::removePermission);
                eventPublisher.publish(new DepartmentPermissionsRevokedEvent(departmentId, removedPermissions));
            }
        }
        
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    @Transactional
    public void deleteDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 부서가 존재하지 않습니다."));
        departmentRepository.delete(department);
    }
}
