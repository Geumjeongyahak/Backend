package geumjeongyahak.domain.users.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.util.Map;

public class UserDeactivatedEvent extends BaseEventDto {

    private final Long userId;

    public UserDeactivatedEvent(Long userId) {
        this.userId = userId;
    }

    public Long userId() {
        return userId;
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of("userId", userId);
    }
}
