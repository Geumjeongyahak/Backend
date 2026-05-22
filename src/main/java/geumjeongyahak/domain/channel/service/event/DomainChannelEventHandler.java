package geumjeongyahak.domain.channel.service.event;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.event.DepartmentChannelProvisionedEvent;
import geumjeongyahak.domain.channel.service.SystemChannelService;
import geumjeongyahak.domain.classroom.event.ClassroomCreatedEvent;
import geumjeongyahak.domain.classroom.event.ClassroomDeletedEvent;
import geumjeongyahak.domain.department.event.DepartmentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainChannelEventHandler {

    private final SystemChannelService systemChannelService;
    private final EventPublisher eventPublisher;

    @EventListener
    @Transactional
    public void handleDepartmentCreated(DepartmentCreatedEvent event) {
        Channel channel = systemChannelService.ensureDepartmentChannel(event.departmentId(), event.departmentName());
        log.info("부서 채널 생성 완료 - departmentId: {}, channelId: {}", event.departmentId(), channel.getId());
        eventPublisher.publish(new DepartmentChannelProvisionedEvent(event.departmentId(), channel.getId()));
    }

    @EventListener
    @Transactional
    public void handleClassroomCreated(ClassroomCreatedEvent event) {
        systemChannelService.ensureClassroomChannel(event.classroomId(), event.classroomName());
        log.info("분반 채널 생성 완료 - classroomId: {}", event.classroomId());
    }

    @EventListener
    @Transactional
    public void handleClassroomDeleted(ClassroomDeletedEvent event) {
        systemChannelService.deactivateClassroomChannel(event.classroomId());
        log.info("분반 채널 비활성화 완료 - classroomId: {}", event.classroomId());
    }
}
