package geumjeongyahak.domain.request.event;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import geumjeongyahak.common.event.dto.BaseEventDto;

@Getter
public class SubjectApprovedEvent extends BaseEventDto {

    private final Long subjectId;
    private final Long newTeacherId;
    private final LocalDate approvalDate;

    public SubjectApprovedEvent(Long subjectId, Long newTeacherId, LocalDate approvalDate) {
        this.subjectId = subjectId;
        this.newTeacherId = newTeacherId;
        this.approvalDate = approvalDate;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("subjectId", subjectId);
        data.put("newTeacherId", newTeacherId);
        data.put("approvalDate", approvalDate);
        return data;
    }
}
