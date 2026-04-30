package geumjeongyahak.domain.department.event;

import geumjeongyahak.common.event.dto.BaseEventDto;

import java.util.Map;

public class DepartmentCreatedEvent extends BaseEventDto {

    private final Long departmentId;

    public DepartmentCreatedEvent(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long departmentId() {
        return departmentId;
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of("departmentId", departmentId);
    }
}
