package geumjeongyahak.unit.channel;

import static org.mockito.Mockito.verify;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.channel.service.SystemChannelService;
import geumjeongyahak.domain.channel.service.event.DomainChannelEventHandler;
import geumjeongyahak.domain.classroom.event.ClassroomDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DomainChannelEventHandlerTest {

    @Mock
    private SystemChannelService systemChannelService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private DomainChannelEventHandler domainChannelEventHandler;

    @Test
    void handleClassroomDeleted_deactivatesClassroomChannel() {
        domainChannelEventHandler.handleClassroomDeleted(new ClassroomDeletedEvent(10L));

        verify(systemChannelService).deactivateClassroomChannel(10L);
    }
}
