package sonmoeum.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
