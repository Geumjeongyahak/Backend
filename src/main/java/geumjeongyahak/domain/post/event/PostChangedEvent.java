package geumjeongyahak.domain.post.event;

import lombok.Getter;
import geumjeongyahak.common.event.dto.BaseEventDto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PostChangedEvent extends BaseEventDto {
    private final Long channelId;
    private final LocalDateTime lastPostedAt;
    private final Map<String, Object> eventData;

    public PostChangedEvent(Long channelId, LocalDateTime lastPostedAt) {
        this.channelId = channelId;
        this.lastPostedAt = lastPostedAt;
        this.eventData = new HashMap<>();
        this.eventData.put("channelId", channelId);
        this.eventData.put("lastPostedAt", lastPostedAt);
    }

    @Override
    public Map<String, Object> getEventData() {
        return eventData;
    }
}
