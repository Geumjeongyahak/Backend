package geumjeongyahak.domain.users.service.event;

import geumjeongyahak.domain.teacher_application.event.TeacherApprovedEvent;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TeacherApplicationEventHandler {

    private final UserProxyService userProxyService;

    @EventListener
    @Transactional
    public void handleTeacherApproved(TeacherApprovedEvent event) {
        User user = userProxyService.getById(event.userId());

        user.approveTeacherProfile(
            event.teacherStartAt(),
            event.teacherEndAt()
        );
    }
}
