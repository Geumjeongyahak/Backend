package geumjeongyahak.domain.department.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.exception.DeleteDepartmentWithMemberException;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.department.v1.dto.request.CreateDepartmentRequest;
import geumjeongyahak.domain.department.v1.dto.request.UpdateDepartmentRequest;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentDetailResponse;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentListResponse;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentSimpleResponse;
import geumjeongyahak.domain.users.service.UserProxyService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentCrudService {
    private final DepartmentRepository departmentRepository;
    private final DepartmentPermissionService departmentPermissionService;
    private final UserProxyService userProxyService;

    @Transactional
    public DepartmentSimpleResponse createDepartment(CreateDepartmentRequest request) {
        log.debug("부서 생성 요청 - 이름: {}", request.name());

        Department department = Department.builder()
                .name(request.name())
                .description(request.description())
                .build();
        departmentPermissionService.replacePermissions(department, request.permissions());
        Department savedDepartment = departmentRepository.save(department);
        log.info("부서 생성 완료 - ID: {}", savedDepartment.getId());
        return DepartmentSimpleResponse.from(savedDepartment);
    }

    public DepartmentDetailResponse getDepartmentDetailById(Long deptId) {
        log.debug("부서 상세 조회 요청 - ID: {}", deptId);
        Department department = findDepartmentById(deptId);
        List<geumjeongyahak.domain.users.entity.User> users = userProxyService.getAllByDepartmentId(deptId);
        log.debug("부서 상세 조회 완료 - ID: {}, 사용자 수: {}", deptId, users.size());
        return DepartmentDetailResponse.from(department, users);
    }

    public DepartmentListResponse getAllDepartments() {
        log.debug("전체 부서 목록 조회 요청");
        List<Department> departments = departmentRepository.findAll();
        log.debug("전체 부서 목록 조회 완료 - 총 {}개", departments.size());
        return DepartmentListResponse.from(departments);
    }

    @Transactional
    public DepartmentSimpleResponse updateDepartment(Long deptId, UpdateDepartmentRequest request) {
        log.debug("부서 수정 요청 - ID: {}", deptId);
        Department department = findDepartmentById(deptId);
        if (request.name() != null) {
            department.setName(request.name());
        }
        if (request.description() != null) {
            department.setDescription(request.description());
        }
        if (request.permissions() != null) {
            departmentPermissionService.replacePermissions(department, request.permissions());
        }
        Department updatedDepartment = departmentRepository.save(department);
        log.info("부서 수정 완료 - ID: {}", updatedDepartment.getId());
        return DepartmentSimpleResponse.from(updatedDepartment);
    }

    @Transactional
    public void deleteDepartment(Long deptId) {
        log.debug("부서 삭제 요청 - ID: {}", deptId);
        Department department = findDepartmentById(deptId);
        if (userProxyService.existsByDepartmentId(deptId)) {
            log.warn("부서에 할당된 사용자가 있어 삭제할 수 없습니다 - ID: {}", deptId);
            throw new DeleteDepartmentWithMemberException(deptId);
        }
        departmentRepository.delete(department);
        log.info("부서 삭제 완료 - ID: {}", deptId);
    }

    private Department findDepartmentById(Long deptId) {
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> {
                    log.warn("부서를 찾을 수 없습니다 - ID: {}", deptId);
                    return new ResourceNotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND, "부서를 찾을 수 없습니다 - ID: " + deptId);
                });
    }
}
