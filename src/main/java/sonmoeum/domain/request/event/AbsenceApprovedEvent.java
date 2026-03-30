package sonmoeum.domain.request.event;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import sonmoeum.common.event.dto.BaseEventDto;

@Getter
public class AbsenceApprovedEvent extends BaseEventDto {

    private final Long lessonId;
    private final Long approverId;

    public AbsenceApprovedEvent(Long lessonId, Long approverId) {
        this.lessonId = lessonId;
        this.approverId = approverId;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("lessonId", lessonId);
        data.put("approverId", approverId);
        return data;
    }
}
