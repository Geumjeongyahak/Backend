package geumjeongyahak.domain.daily_schedule.service.event;

import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.lesson.event.LessonDailyScheduleSyncRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonEventHandler {

    private final DailyScheduleService dailyScheduleService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleLessonDailyScheduleSyncRequested(LessonDailyScheduleSyncRequestedEvent event) {
        log.info(
            "수업 변경 이벤트 처리 - DailySchedule 동기화 (classroomId={}, lessonDate={})",
            event.getClassroomId(),
            event.getLessonDate()
        );
        dailyScheduleService.synchronizeByClassroomAndDate(event.getClassroomId(), event.getLessonDate());
    }
}
