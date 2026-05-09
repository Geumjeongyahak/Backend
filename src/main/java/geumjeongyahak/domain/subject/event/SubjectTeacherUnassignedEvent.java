package geumjeongyahak.domain.subject.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class SubjectTeacherUnassignedEvent extends BaseEventDto {

    private final Long subjectId;
    private final LocalDate effectiveFrom;

    public SubjectTeacherUnassignedEvent(Long subjectId, LocalDate effectiveFrom) {
        this.subjectId = subjectId;
        this.effectiveFrom = effectiveFrom;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("subjectId", subjectId);
        data.put("effectiveFrom", effectiveFrom);
        return data;
    }
}
