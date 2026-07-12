package geumjeongyahak.domain.users.service.event;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.subject.event.SubjectCreatedEvent;
import geumjeongyahak.domain.subject.event.SubjectDeletedEvent;
import geumjeongyahak.domain.subject.event.SubjectTeacherAssignedEvent;
import geumjeongyahak.domain.subject.event.SubjectTeacherUnassignedEvent;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SubjectTeacherAssignmentEventHandler {

    private final UserProxyService userProxyService;
    private final ClassroomProxyService classroomProxyService;
    private final SubjectProxyService subjectProxyService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSubjectCreated(SubjectCreatedEvent event) {
        fillDefaultClassroomIfMissing(event.getTeacherId(), event.getClassroomId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSubjectTeacherAssigned(SubjectTeacherAssignedEvent event) {
        fillDefaultClassroomIfMissing(event.getTeacherId(), event.getClassroomId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSubjectTeacherUnassigned(SubjectTeacherUnassignedEvent event) {
        releaseClassroomIfNoActiveAssignedSubjects(event.getTeacherId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSubjectDeleted(SubjectDeletedEvent event) {
        releaseClassroomIfNoActiveAssignedSubjects(event.getTeacherId());
    }

    private void fillDefaultClassroomIfMissing(Long teacherId, Long classroomId) {
        if (teacherId == null || classroomId == null) {
            return;
        }

        User teacher = userProxyService.getById(teacherId);
        if (teacher.getClassroom() != null) {
            return;
        }

        Classroom classroom = classroomProxyService.getActiveById(classroomId);
        teacher.setClassroom(classroom);
    }

    private void releaseClassroomIfNoActiveAssignedSubjects(Long teacherId) {
        if (teacherId == null || subjectProxyService.existsActiveSubjectByTeacherId(teacherId)) {
            return;
        }

        User teacher = userProxyService.getById(teacherId);
        teacher.setClassroom(null);
    }
}
