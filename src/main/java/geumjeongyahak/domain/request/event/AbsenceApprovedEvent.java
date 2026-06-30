package geumjeongyahak.domain.request.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class AbsenceApprovedEvent extends BaseEventDto {

    private final Long requestId;
    private final Long dailyScheduleId;

    public AbsenceApprovedEvent(
        Long requestId,
        Long dailyScheduleId
    ) {
        this.requestId = requestId;
        this.dailyScheduleId = dailyScheduleId;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("dailyScheduleId", dailyScheduleId);
        return data;
    }
}
