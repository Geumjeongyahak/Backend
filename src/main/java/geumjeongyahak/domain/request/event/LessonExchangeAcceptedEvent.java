package geumjeongyahak.domain.request.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class LessonExchangeAcceptedEvent extends BaseEventDto {

    private final Long lessonId;
    private final Long newTeacherId;

    public LessonExchangeAcceptedEvent(
        Long lessonId,
        Long newTeacherId
    ) {
        this.lessonId = lessonId;
        this.newTeacherId = newTeacherId;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("lessonId", lessonId);
        data.put("newTeacherId", newTeacherId);
        return data;
    }
}
