package geumjeongyahak.domain.lesson.service.schedule;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.event.LessonDailyScheduleSyncRequestedEvent;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 수업이 바뀐 (분반, 날짜)마다 DailySchedule 동기화를 한 번만 요청한다.
 * 수업 여러 개를 한꺼번에 바꾸는 메서드는 끝에서 한 번 부른다.
 */
@Component
@RequiredArgsConstructor
public class DailyScheduleSyncPublisher {

    private final EventPublisher eventPublisher;

    public void publishFor(Collection<Lesson> lessons) {
        publish(lessons.stream().map(ClassroomDate::of).toList());
    }

    public void publish(Collection<ClassroomDate> keys) {
        new LinkedHashSet<>(keys).forEach(key -> eventPublisher.publish(
            new LessonDailyScheduleSyncRequestedEvent(key.classroomId(), key.date())
        ));
    }

    /** DailySchedule 하나가 대응하는 단위. */
    public record ClassroomDate(Long classroomId, LocalDate date) {

        public static ClassroomDate of(Lesson lesson) {
            return new ClassroomDate(lesson.getSubject().getClassroom().getId(), lesson.getDate());
        }
    }
}
