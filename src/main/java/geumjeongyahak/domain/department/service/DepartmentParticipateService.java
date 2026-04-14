package geumjeongyahak.domain.department.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.DuplicateResourceException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.department.entity.UserDepartment;
import geumjeongyahak.domain.department.exception.DepartmentErrorCode;
import geumjeongyahak.domain.department.event.JoinDepartmentEvent;
import geumjeongyahak.domain.department.event.LeaveDepartmentEvent;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.department.repository.UserDepartmentRepository;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentListResponse;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentResponse;
import geumjeongyahak.domain.users.exception.UserErrorCode;
import geumjeongyahak.domain.users.service.UserProxyService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentParticipateService {
    private final UserProxyService userProxyService;
    private final DepartmentRepository departmentRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final EventPublisher eventPublisher;

    public void joinDepartment(Long userId, Long departmentId) {
        log.debug("사용자(ID: {}) 부서 참여 요청 - 부서 ID: {}", userId, departmentId);

        // 사용자 존재 여부 확인 및 조회
        var user = userProxyService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(UserErrorCode.USER_NOT_FOUND));

        // 부서 존재 여부 확인 및 조회
        var department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(DepartmentErrorCode.DEPARTMENT_NOT_FOUND, departmentId));

        // 중복 체크
        if (userDepartmentRepository.existsByUserIdAndDepartmentId(userId, departmentId)) {
            throw new DuplicateResourceException(CommonErrorCode.DUPLICATE_RESOURCE, "이미 해당 부서에 참여한 사용자입니다.");
        }
        // 참여 처리
        UserDepartment userDepartment = new UserDepartment(user, department);
        userDepartmentRepository.save(userDepartment);
        log.info("사용자(ID: {}) 부서 참여 완료 - 부서 ID: {}", userId, departmentId);

        // 이벤트 발행
        eventPublisher.publish(new JoinDepartmentEvent(userId, departmentId, department.getRoleId()));
    }

    public void leaveDepartment(Long userId, Long departmentId) {
        log.debug("사용자(ID: {}) 부서 탈퇴 요청 - 부서 ID: {}", userId, departmentId);
        var department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(DepartmentErrorCode.DEPARTMENT_NOT_FOUND, departmentId));

        if (!userDepartmentRepository.existsByUserIdAndDepartmentId(userId, departmentId)) {
            throw new ResourceNotFoundException(DepartmentErrorCode.USER_NOT_IN_DEPARTMENT);
        }
        userDepartmentRepository.deleteByUserIdAndDepartmentId(userId, departmentId);
        log.info("사용자(ID: {}) 부서 탈퇴 완료 - 부서 ID: {}", userId, departmentId);

        // 이벤트 발행
        eventPublisher.publish(new LeaveDepartmentEvent(userId, departmentId, department.getRoleId()));
    }

    public DepartmentListResponse getUserDepartments(Long userId) {
        log.debug("사용자(ID: {}) 소속 부서 목록 조회 요청", userId);
        // 사용자 존재 여부 확인
        if (!userProxyService.existsById(userId)) {
            throw new ResourceNotFoundException(UserErrorCode.USER_NOT_FOUND);
        }

        List<UserDepartment> userDepartments = userDepartmentRepository.findByUserId(userId);
        List<DepartmentResponse> departments = userDepartments.stream()
                .map(ud -> DepartmentResponse.from(ud.getDepartment()))
                .toList();

        log.debug("사용자(ID: {}) 소속 부서 목록 조회 완료 - 총 {}개", userId, departments.size());
        return new DepartmentListResponse(departments);
    }

}
