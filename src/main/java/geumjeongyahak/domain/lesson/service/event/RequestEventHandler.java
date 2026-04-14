package geumjeongyahak.domain.lesson.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import geumjeongyahak.domain.lesson.service.LessonService;
import geumjeongyahak.domain.request.event.AbsenceApprovedEvent;
import geumjeongyahak.domain.request.event.LessonExchangeApprovedEvent;
import geumjeongyahak.domain.request.event.SubjectApprovedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestEventHandler {

    private final LessonService lessonService;

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleAbsenceApproved(AbsenceApprovedEvent event) {
        log.info("결석 승인 이벤트 처리 - 교사 출석 상태 공결 처리 (lessonId={})", event.getLessonId());
        lessonService.applyTeacherExcused(event.getLessonId());
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleLessonExchangeApproved(LessonExchangeApprovedEvent event) {
        log.info("수업 교환 승인 이벤트 처리 - 담당 교사 교환 (lessonId={}, requesterId={}, newTeacherId={})",
            event.getLessonId(), event.getRequesterId(), event.getNewTeacherId());
        lessonService.applyTeacherExchange(event.getLessonId(), event.getRequesterId(), event.getNewTeacherId());
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSubjectApproved(SubjectApprovedEvent event) {
        log.info("과목 교환 승인 이벤트 처리 - 수업 교사 일괄 변경 (subjectId={}, newTeacherId={}, from={})",
            event.getSubjectId(), event.getNewTeacherId(), event.getApprovalDate());
        lessonService.applyTeacherChangeFromSubjectApproval(
            event.getSubjectId(), event.getNewTeacherId(), event.getApprovalDate()
        );
    }
}
