package geumjeongyahak.domain.request.event;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import geumjeongyahak.common.event.dto.BaseEventDto;

@Getter
public class LessonExchangeApprovedEvent extends BaseEventDto {

    private final Long lessonId;
    private final Long requesterId;
    private final Long newTeacherId;
    private final Long approverId;

    public LessonExchangeApprovedEvent(Long lessonId, Long requesterId, Long newTeacherId, Long approverId) {
        this.lessonId = lessonId;
        this.requesterId = requesterId;
        this.newTeacherId = newTeacherId;
        this.approverId = approverId;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("lessonId", lessonId);
        data.put("requesterId", requesterId);
        data.put("newTeacherId", newTeacherId);
        data.put("approverId", approverId);
        return data;
    }
}
