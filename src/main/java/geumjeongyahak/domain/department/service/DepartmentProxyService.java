package geumjeongyahak.domain.department.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.exception.DepartmentErrorCode;
import geumjeongyahak.domain.department.repository.DepartmentRepository;

/**
 * Department 도메인의 Proxy Service.
 * 다른 도메인에서 부서 참조 확인이 필요할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class DepartmentProxyService {

    private final DepartmentRepository departmentRepository;

    /**
     * 부서 조회. 없으면 예외 발생.
     */
    @Transactional(readOnly = true)
    public Department getById(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(DepartmentErrorCode.DEPARTMENT_NOT_FOUND));
    }
}
