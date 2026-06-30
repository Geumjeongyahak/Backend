package geumjeongyahak.domain.department.service;

import geumjeongyahak.domain.department.entity.DepartmentPermission;
import geumjeongyahak.domain.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Department permission 도메인의 Proxy Service.
 * 다른 도메인에서 사용자의 부서 직책 권한 조회가 필요할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class DepartmentPermissionProxyService {

    private final DepartmentPermissionService departmentPermissionService;

    @Transactional(readOnly = true)
    public List<DepartmentPermission> getEffectivePermissions(User user) {
        return departmentPermissionService.getEffectivePermissions(user);
    }
}
