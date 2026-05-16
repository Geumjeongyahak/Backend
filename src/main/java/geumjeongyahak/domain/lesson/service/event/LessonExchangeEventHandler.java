package geumjeongyahak.domain.lesson.service.event;

import geumjeongyahak.domain.lesson.service.LessonService;
import geumjeongyahak.domain.request.event.LessonExchangeAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonExchangeEventHandler {

    private final LessonService lessonService;

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
