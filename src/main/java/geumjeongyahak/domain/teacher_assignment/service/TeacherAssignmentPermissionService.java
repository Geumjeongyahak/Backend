package geumjeongyahak.domain.teacher_assignment.service;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.base.model.PermissionCode;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.service.ChannelProxyService;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.users.service.UserPermissionService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherAssignmentPermissionService {

    private final ChannelProxyService channelProxyService;
    private final SubjectProxyService subjectProxyService;
    private final UserPermissionService userPermissionService;

    public void addClassroomChannelWritePermission(Long teacherId, Long classroomId) {
        userPermissionService.addPermission(teacherId, classroomChannelWritePermissionCode(classroomId));
    }

    public Map<Long, Set<Long>> collectAssignedTeacherIdsByClassroom(List<Subject> subjects) {
        Map<Long, Set<Long>> teacherIdsByClassroomId = new HashMap<>();
        subjects.stream()
            .filter(subject -> subject.getTeacher() != null)
            .forEach(subject -> teacherIdsByClassroomId
                .computeIfAbsent(subject.getClassroom().getId(), ignored -> new HashSet<>())
                .add(subject.getTeacher().getId()));
        return teacherIdsByClassroomId;
    }

    public void removeUnusedClassroomChannelWritePermissions(Map<Long, Set<Long>> teacherIdsByClassroomId) {
        teacherIdsByClassroomId.forEach((classroomId, teacherIds) -> teacherIds.forEach(teacherId -> {
            if (subjectProxyService.existsActiveSubjectByClassroomIdAndTeacherId(classroomId, teacherId)) {
                return;
            }
            userPermissionService.removePermission(teacherId, classroomChannelWritePermissionCode(classroomId));
        }));
    }

    private String classroomChannelWritePermissionCode(Long classroomId) {
        Channel channel = channelProxyService.getActiveDomainLinkedClassroomChannel(classroomId);
        return PermissionCode.of(ResourceType.CHANNEL, ActionType.WRITE, channel.getId()).value();
    }
}
