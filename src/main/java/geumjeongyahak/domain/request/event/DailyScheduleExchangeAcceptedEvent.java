package geumjeongyahak.domain.request.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class DailyScheduleExchangeAcceptedEvent extends BaseEventDto {

    private final Long dailyScheduleId;
    private final Long newTeacherId;
    private final LocalDate exchangedLessonDate;

    public DailyScheduleExchangeAcceptedEvent(
        Long dailyScheduleId,
        Long newTeacherId,
        LocalDate exchangedLessonDate
    ) {
        this.dailyScheduleId = dailyScheduleId;
        this.newTeacherId = newTeacherId;
        this.exchangedLessonDate = exchangedLessonDate;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("dailyScheduleId", dailyScheduleId);
        data.put("newTeacherId", newTeacherId);
        data.put("exchangedLessonDate", exchangedLessonDate);
        return data;
    }
}
