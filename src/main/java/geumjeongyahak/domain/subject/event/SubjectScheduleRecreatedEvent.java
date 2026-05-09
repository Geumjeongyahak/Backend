package geumjeongyahak.domain.subject.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class SubjectScheduleRecreatedEvent extends BaseEventDto {

    private final Long subjectId;
    private final Long teacherId;
    private final LocalDate effectiveFrom;
    private final LocalDate startAt;
    private final LocalDate endAt;
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer period;

    public SubjectScheduleRecreatedEvent(
        Long subjectId,
        Long teacherId,
        LocalDate effectiveFrom,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer period
    ) {
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.effectiveFrom = effectiveFrom;
        this.startAt = startAt;
        this.endAt = endAt;
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
        data.put("effectiveFrom", effectiveFrom);
        data.put("startAt", startAt);
        data.put("endAt", endAt);
        data.put("dayOfWeek", dayOfWeek != null ? dayOfWeek.name() : null);
        data.put("startTime", startTime);
        data.put("endTime", endTime);
        data.put("period", period);
        return data;
    }
}
