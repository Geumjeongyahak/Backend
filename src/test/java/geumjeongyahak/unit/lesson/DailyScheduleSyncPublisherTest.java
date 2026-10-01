package geumjeongyahak.unit.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.event.dto.BaseEventDto;
import geumjeongyahak.domain.lesson.event.LessonDailyScheduleSyncRequestedEvent;
import geumjeongyahak.domain.lesson.service.schedule.DailyScheduleSyncPublisher;
import geumjeongyahak.domain.lesson.service.schedule.DailyScheduleSyncPublisher.ClassroomDate;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyScheduleSyncPublisherTest {

    private static final LocalDate MONDAY = LocalDate.of(2099, 3, 2);

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private DailyScheduleSyncPublisher publisher;

    @Test
    void publish_requestsEachClassroomDateOnceInFirstSeenOrder() {
        publisher.publish(List.of(
            new ClassroomDate(1L, MONDAY),
            new ClassroomDate(2L, MONDAY),
            new ClassroomDate(1L, MONDAY),
            new ClassroomDate(1L, MONDAY.plusWeeks(1))
        ));

        ArgumentCaptor<BaseEventDto> events = ArgumentCaptor.forClass(BaseEventDto.class);
        verify(eventPublisher, times(3)).publish(events.capture());
        assertThat(events.getAllValues())
            .map(event -> (LessonDailyScheduleSyncRequestedEvent) event)
            .extracting(event -> new ClassroomDate(event.getClassroomId(), event.getLessonDate()))
            .containsExactly(
                new ClassroomDate(1L, MONDAY),
                new ClassroomDate(2L, MONDAY),
                new ClassroomDate(1L, MONDAY.plusWeeks(1))
            );
    }

    @Test
    void publish_doesNothingForNoKeys() {
        publisher.publish(List.of());

        verifyNoInteractions(eventPublisher);
    }
}
