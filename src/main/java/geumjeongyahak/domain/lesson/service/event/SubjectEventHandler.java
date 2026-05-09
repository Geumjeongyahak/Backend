package geumjeongyahak.domain.lesson.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import geumjeongyahak.domain.lesson.service.LessonService;
import geumjeongyahak.domain.subject.event.SubjectCreatedEvent;
import geumjeongyahak.domain.subject.event.SubjectTeacherAssignedEvent;
import geumjeongyahak.domain.subject.event.SubjectTeacherUnassignedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectEventHandler {

    private final LessonService lessonService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSubjectCreated(SubjectCreatedEvent event) {
        log.info("과목 생성 이벤트 처리 - 수업 자동 생성 (subjectId={}, times={})",
            event.getSubjectId(), event.getTimes());
        lessonService.createLessonsFromSubject(
            event.getSubjectId(),
            event.getTeacherId(),
            event.getStartAt(),
            event.getEndAt(),
            event.getTimes(),
            event.getDayOfWeek(),
            event.getStartTime(),
            event.getEndTime(),
            event.getPeriod()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSubjectTeacherAssigned(SubjectTeacherAssignedEvent event) {
        log.info(
            "과목 담당 교사 배정 이벤트 처리 - 수업 교사 변경 (subjectId={}, teacherId={})",
            event.getSubjectId(),
            event.getTeacherId()
        );
        lessonService.assignTeacherToSubjectScheduledLessons(
            event.getSubjectId(),
            event.getTeacherId(),
            event.getEffectiveFrom()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSubjectTeacherUnassigned(SubjectTeacherUnassignedEvent event) {
        log.info(
            "과목 담당 교사 해제 이벤트 처리 - 수업 삭제 (subjectId={})",
            event.getSubjectId()
        );
        lessonService.deleteSubjectScheduledLessons(
            event.getSubjectId(),
            event.getEffectiveFrom()
        );
    }
}
