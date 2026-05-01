package geumjeongyahak.domain.lesson.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import geumjeongyahak.domain.request.event.LessonExchangeAcceptedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import geumjeongyahak.domain.lesson.service.LessonService;
import geumjeongyahak.domain.request.event.AbsenceApprovedEvent;

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
    public void handleLessonExchangeAccepted(LessonExchangeAcceptedEvent event) {
        log.info(
            "수업 교환 수락 이벤트 처리 - 실제 수업 담당 교사 변경 (lessonId={}, newTeacherId={})",
            event.getLessonId(),
            event.getNewTeacherId()
        );
        lessonService.applyTeacherExchange(event.getLessonId(), event.getNewTeacherId());
    }

}
