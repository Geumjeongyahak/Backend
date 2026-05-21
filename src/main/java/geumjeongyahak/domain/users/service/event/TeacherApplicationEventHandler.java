package geumjeongyahak.domain.users.service.event;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.base.model.PermissionCode;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.service.ChannelProxyService;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.teacher_application.event.TeacherApprovedEvent;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserPermissionService;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TeacherApplicationEventHandler {

    private final UserProxyService userProxyService;
    private final ClassroomProxyService classroomProxyService;
    private final ChannelProxyService channelProxyService;
    private final UserPermissionService userPermissionService;

    @EventListener
    @Transactional
    public void handleTeacherApproved(TeacherApprovedEvent event) {
        User user = userProxyService.getById(event.userId());
        Classroom classroom = classroomProxyService.getActiveById(event.classroomId());

        user.approveTeacherProfile(
            classroom,
            event.teacherStartAt(),
            event.teacherEndAt()
        );

        Channel channel = channelProxyService.getActiveDomainLinkedClassroomChannel(event.classroomId());
        String permissionCode = PermissionCode.of(ResourceType.CHANNEL, ActionType.WRITE, channel.getId()).value();
        userPermissionService.addPermission(user.getId(), permissionCode);
    }
}
