package geumjeongyahak.domain.daily_schedule.service.event;

import geumjeongyahak.domain.daily_schedule.service.DailyScheduleSynchronizer;
import geumjeongyahak.domain.lesson.event.LessonDailyScheduleSyncRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonEventHandler {

    private final DailyScheduleSynchronizer synchronizer;

    @EventListener
    public void handleLessonDailyScheduleSyncRequested(LessonDailyScheduleSyncRequestedEvent event) {
        log.info(
            "수업 변경 이벤트 처리 - DailySchedule 동기화 (classroomId={}, lessonDate={})",
            event.getClassroomId(),
            event.getLessonDate()
        );
        synchronizer.synchronize(event.getClassroomId(), event.getLessonDate());
    }
}
