package sonmoeum.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import sonmoeum.common.event.dto.BaseEventDto;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(BaseEventDto dto) {
        log.info("이벤트 발행: {}", dto.toString());
        applicationEventPublisher.publishEvent(dto);
    }
}
