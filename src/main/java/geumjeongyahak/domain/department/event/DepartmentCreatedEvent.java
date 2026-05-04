package geumjeongyahak.domain.department.event;

import geumjeongyahak.common.event.dto.BaseEventDto;

import java.util.Map;

public class DepartmentCreatedEvent extends BaseEventDto {

    private final Long departmentId;
    private final String departmentName;

    public DepartmentCreatedEvent(Long departmentId, String departmentName) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }

    public Long departmentId() {
        return departmentId;
    }

    public String departmentName() {
        return departmentName;
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of("departmentId", departmentId, "departmentName", departmentName);
    }
}
