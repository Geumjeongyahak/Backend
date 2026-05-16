package geumjeongyahak.domain.daily_schedule.service.event;

import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.request.event.AbsenceApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyScheduleRequestEventHandler {

    private final DailyScheduleService dailyScheduleService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleAbsenceApproved(AbsenceApprovedEvent event) {
        log.info(
            "결석 승인 이벤트 처리 - DailySchedule 교사 출석 공결 반영 (requestId={}, dailyScheduleId={})",
            event.getRequestId(),
            event.getDailyScheduleId()
        );
        dailyScheduleService.applyApprovedAbsence(event.getDailyScheduleId());
    }
}
