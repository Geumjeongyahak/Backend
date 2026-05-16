package geumjeongyahak.domain.daily_schedule.service.event;

import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.request.event.DailyScheduleExchangeAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyScheduleExchangeEventHandler {

    private final DailyScheduleService dailyScheduleService;

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleDailyScheduleExchangeAccepted(DailyScheduleExchangeAcceptedEvent event) {
        log.info(
            "수업 교환 수락 이벤트 처리 - DailySchedule 담당 교사 변경 (dailyScheduleId={}, newTeacherId={})",
            event.getDailyScheduleId(),
            event.getNewTeacherId()
        );
        dailyScheduleService.applyTeacherExchange(event.getDailyScheduleId(), event.getNewTeacherId());
    }
}
