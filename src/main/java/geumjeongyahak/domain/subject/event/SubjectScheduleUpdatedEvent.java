package geumjeongyahak.domain.subject.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class SubjectScheduleUpdatedEvent extends BaseEventDto {

    private final Long subjectId;
    private final LocalDate effectiveFrom;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer period;

    public SubjectScheduleUpdatedEvent(
        Long subjectId,
        LocalDate effectiveFrom,
        LocalTime startTime,
        LocalTime endTime,
        Integer period
    ) {
        this.subjectId = subjectId;
        this.effectiveFrom = effectiveFrom;
        this.startTime = startTime;
        this.endTime = endTime;
        this.period = period;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("subjectId", subjectId);
        data.put("effectiveFrom", effectiveFrom);
        data.put("startTime", startTime);
        data.put("endTime", endTime);
        data.put("period", period);
        return data;
    }
}
