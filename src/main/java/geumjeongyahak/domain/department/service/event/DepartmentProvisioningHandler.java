package geumjeongyahak.domain.department.service.event;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.base.model.PermissionCode;
import geumjeongyahak.domain.channel.event.DepartmentChannelProvisionedEvent;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.service.DepartmentPermissionService;
import geumjeongyahak.domain.department.service.DepartmentProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentProvisioningHandler {

    private final DepartmentProxyService departmentProxyService;
    private final DepartmentPermissionService departmentPermissionService;

    @EventListener
    @Transactional
    public void handleDepartmentChannelProvisioned(DepartmentChannelProvisionedEvent event) {
        Department department = departmentProxyService.getById(event.departmentId());

        departmentPermissionService.addMemberPermission(department, PermissionCode.of(
                ResourceType.CHANNEL, ActionType.READ, event.channelId()).toString());

        departmentPermissionService.addMemberPermission(department, PermissionCode.of(
                ResourceType.CHANNEL, ActionType.WRITE, event.channelId()).toString());

        log.info("부서 기본 권한 추가 완료 - departmentId: {}", department.getId());
    }
}
