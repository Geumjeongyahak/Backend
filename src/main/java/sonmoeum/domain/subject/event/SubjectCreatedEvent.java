package sonmoeum.domain.subject.event;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import sonmoeum.common.event.dto.BaseEventDto;

@Getter
public class SubjectCreatedEvent extends BaseEventDto {

    private final Long subjectId;
    private final Long teacherId;
    private final LocalDate startAt;
    private final LocalDate endAt;
    private final Integer times;
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer period;

    public SubjectCreatedEvent(
        Long subjectId,
        Long teacherId,
        LocalDate startAt,
        LocalDate endAt,
        Integer times,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer period
    ) {
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.times = times;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.period = period;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("subjectId", subjectId);
        data.put("teacherId", teacherId);
        data.put("startAt", startAt);
        data.put("endAt", endAt);
        data.put("times", times);
        data.put("dayOfWeek", dayOfWeek);
        data.put("period", period);
        return data;
    }
}
