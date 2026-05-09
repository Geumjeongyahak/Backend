package geumjeongyahak.domain.post.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import lombok.Getter;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
public class PostImageDeleteRequestedEvent extends BaseEventDto {

    private final Long postId;
    private final Set<UUID> fileIds;

    public PostImageDeleteRequestedEvent(Long postId, Set<UUID> fileIds) {
        this.postId = postId;
        this.fileIds = Set.copyOf(fileIds);
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of(
                "postId", postId,
                "fileIds", fileIds
        );
    }
}
