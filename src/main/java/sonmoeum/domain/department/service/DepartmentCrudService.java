package sonmoeum.domain.department.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.common.exception.CommonErrorCode;
import sonmoeum.common.exception.ResourceNotFoundException;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.department.entity.Department;
import sonmoeum.domain.department.entity.UserDepartment;
import sonmoeum.domain.department.exception.DeleteDepartmentWithMemberException;
import sonmoeum.domain.department.exception.DeleteDepartmentWithRoleException;
import sonmoeum.domain.department.repository.DepartmentRepository;
import sonmoeum.domain.department.repository.UserDepartmentRepository;
import sonmoeum.domain.department.v1.dto.request.CreateDepartmentRequest;
import sonmoeum.domain.department.v1.dto.request.UpdateDepartmentRequest;
import sonmoeum.domain.department.v1.dto.response.DepartmentDetailResponse;
import sonmoeum.domain.department.v1.dto.response.DepartmentListResponse;
import sonmoeum.domain.department.v1.dto.response.DepartmentResponse;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentCrudService {
    private final UserDepartmentRepository userDepartmentRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        log.debug("부서 생성 요청 - 이름: {}", request.name());

        Department department = Department.builder()
                .name(request.name())
                .description(request.description())
                .build();
        Department savedDepartment = departmentRepository.save(department);
        log.info("부서 생성 완료 - ID: {}", savedDepartment.getId());
        return DepartmentResponse.from(savedDepartment);
    }

    public DepartmentDetailResponse getDepartmentDetailById(Long deptId) {
        log.debug("부서 상세 조회 요청 - ID: {}", deptId);
        Department department = findDepartmentById(deptId);

        List<UserDepartment> userDepartments = userDepartmentRepository.findByDepartmentId(deptId);
        log.debug("부서 상세 조회 완료 - ID: {}, 사용자 수: {}", deptId, userDepartments.size());
        return DepartmentDetailResponse.from(department, userDepartments);
    }

    public DepartmentListResponse getAllDepartments() {
        log.debug("전체 부서 목록 조회 요청");
        List<Department> departments = departmentRepository.findAll();
        log.debug("전체 부서 목록 조회 완료 - 총 {}개", departments.size());
        return DepartmentListResponse.from(departments);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long deptId, UpdateDepartmentRequest request) {
        log.debug("부서 수정 요청 - ID: {}", deptId);
        Department department = findDepartmentById(deptId);
        if (request.name() != null) {
            department.setName(request.name());
        }
        if (request.description() != null) {
            department.setDescription(request.description());
        }
        Department updatedDepartment = departmentRepository.save(department);
        log.info("부서 수정 완료 - ID: {}", updatedDepartment.getId());
        return DepartmentResponse.from(updatedDepartment);
    }

    @Transactional
    public void deleteDepartment(Long deptId) {
        log.debug("부서 삭제 요청 - ID: {}", deptId);
        Department department = findDepartmentById(deptId);
        if (department.getRoleId() != null) {
            log.warn("부서에 할당된 역할이 있어 삭제할 수 없습니다 - ID: {}", deptId);
            throw new DeleteDepartmentWithRoleException(deptId, RoleType.findById(department.getRoleId()));
        }
        if (userDepartmentRepository.existsByDepartmentId(deptId)) {
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
