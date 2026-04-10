package sonmoeum.domain.department.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.department.repository.UserDepartmentRepository;

/**
 * UserDepartment 도메인의 Proxy Service.
 * 다른 도메인에서 사용자-부서 소속 여부 확인이 필요할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class UserDepartmentProxyService {

    private final UserDepartmentRepository userDepartmentRepository;

    @Transactional(readOnly = true)
    public boolean existsByUserIdAndDepartmentId(Long userId, Long departmentId) {
        return userDepartmentRepository.existsByUserIdAndDepartmentId(userId, departmentId);
    }
}
