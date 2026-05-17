package geumjeongyahak.domain.request.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class DailyScheduleExchangeAcceptedEvent extends BaseEventDto {

    private final Long dailyScheduleId;
    private final Long newTeacherId;

    public DailyScheduleExchangeAcceptedEvent(Long dailyScheduleId, Long newTeacherId) {
        this.dailyScheduleId = dailyScheduleId;
        this.newTeacherId = newTeacherId;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("dailyScheduleId", dailyScheduleId);
        data.put("newTeacherId", newTeacherId);
        return data;
    }
}
