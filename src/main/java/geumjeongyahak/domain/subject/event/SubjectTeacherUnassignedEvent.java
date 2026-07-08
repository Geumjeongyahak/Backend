package geumjeongyahak.domain.subject.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class SubjectTeacherUnassignedEvent extends BaseEventDto {

    private final Long subjectId;
    private final Long classroomId;
    private final Long teacherId;
    private final LocalDate effectiveFrom;

    public SubjectTeacherUnassignedEvent(
        Long subjectId,
        Long classroomId,
        Long teacherId,
        LocalDate effectiveFrom
    ) {
        this.subjectId = subjectId;
        this.classroomId = classroomId;
        this.teacherId = teacherId;
        this.effectiveFrom = effectiveFrom;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("subjectId", subjectId);
        data.put("classroomId", classroomId);
        data.put("teacherId", teacherId);
        data.put("effectiveFrom", effectiveFrom);
        return data;
    }
}
