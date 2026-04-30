package geumjeongyahak.domain.department.service.event;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.base.model.PermissionCode;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.service.SystemChannelService;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.event.DepartmentCreatedEvent;
import geumjeongyahak.domain.department.service.DepartmentPermissionService;
import geumjeongyahak.domain.department.service.DepartmentProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentProvisioningHandler {

    private final DepartmentProxyService departmentProxyService;
    private final SystemChannelService systemChannelService;
    private final DepartmentPermissionService departmentPermissionService;

    @EventListener
    public void handleDepartmentCreated(DepartmentCreatedEvent event) {
        Department department = departmentProxyService.getById(event.departmentId());

        Channel channel = systemChannelService.ensureDepartmentChannel(department);
        
        departmentPermissionService.addPermission(department, PermissionCode.of(
            ResourceType.CHANNEL, ActionType.READ, channel.getId()).toString());
        
        departmentPermissionService.addPermission(department, PermissionCode.of(
            ResourceType.CHANNEL, ActionType.WRITE, channel.getId()).toString());

        log.info("부서 기본 권한 추가 완료 - departmentId: {}", department.getId());
    }
}
