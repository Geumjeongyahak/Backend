package sonmoeum.domain.request.event;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import sonmoeum.common.event.dto.BaseEventDto;

@Getter
public class LessonExchangeApprovedEvent extends BaseEventDto {

    private final Long lessonId;
    private final Long newTeacherId;
    private final Long approverId;

    public LessonExchangeApprovedEvent(Long lessonId, Long newTeacherId, Long approverId) {
        this.lessonId = lessonId;
        this.newTeacherId = newTeacherId;
        this.approverId = approverId;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("lessonId", lessonId);
        data.put("newTeacherId", newTeacherId);
        data.put("approverId", approverId);
        return data;
    }
}
