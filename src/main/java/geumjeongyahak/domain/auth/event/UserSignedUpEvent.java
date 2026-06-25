package geumjeongyahak.domain.auth.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.util.Map;

public class UserSignedUpEvent extends BaseEventDto {

    private final Long userId;
    private final String email;
    private final String name;

    public UserSignedUpEvent(Long userId, String email, String name) {
        this.userId = userId;
        this.email = email;
        this.name = name;
    }

    public Long userId() {
        return userId;
    }

    public String email() {
        return email;
    }

    public String name() {
        return name;
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of(
            "userId", userId,
            "email", email,
            "name", name
        );
    }
}
