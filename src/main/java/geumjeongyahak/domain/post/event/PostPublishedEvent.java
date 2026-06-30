package geumjeongyahak.domain.post.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PostPublishedEvent extends BaseEventDto {
    private final Long postId;
    private final Long channelId;
    private final Long authorId;
    private final Map<String, Object> eventData;

    public PostPublishedEvent(Long postId, Long channelId, Long authorId) {
        this.postId = postId;
        this.channelId = channelId;
        this.authorId = authorId;
        this.eventData = new HashMap<>();
        this.eventData.put("postId", postId);
        this.eventData.put("channelId", channelId);
        this.eventData.put("authorId", authorId);
    }

    @Override
    public Map<String, Object> getEventData() {
        return eventData;
    }
}
