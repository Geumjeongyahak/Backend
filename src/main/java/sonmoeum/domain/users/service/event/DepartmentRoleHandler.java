package sonmoeum.domain.users.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import sonmoeum.domain.department.event.JoinDepartmentEvent;
import sonmoeum.domain.department.event.LeaveDepartmentEvent;
import sonmoeum.domain.users.service.UserRoleService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentRoleHandler {
    private final UserRoleService userRoleService;

    @TransactionalEventListener
    public void handleUserJoinedDepartment(JoinDepartmentEvent event) {
        log.info("부서 참여 이벤트 처리 - 사용자에게 부서 역할 부여");
        if (event.getRole() != null) {
            userRoleService.addRoleIfNotExist(event.getUserId(), event.getRole());
        }
    }

    @TransactionalEventListener
    public void handleUserLeaveDepartment(LeaveDepartmentEvent event) {
        log.info("부서 탈퇴 이벤트 처리 - 사용자에게 부서 역할 제거");
        if (event.getRole() != null) {
            userRoleService.removeRoleIfExist(event.getUserId(), event.getRole());
        }
    }
}
