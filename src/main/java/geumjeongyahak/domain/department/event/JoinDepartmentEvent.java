package geumjeongyahak.domain.department.event;

import lombok.Getter;
import geumjeongyahak.common.event.dto.BaseEventDto;
import geumjeongyahak.domain.auth.enums.RoleType;

import java.util.HashMap;
import java.util.Map;

@Getter
public class JoinDepartmentEvent extends BaseEventDto {
    private final Long userId;
    private final Long departmentId;
    private final RoleType role;
    private final Map<String, Object> shownData;

    public JoinDepartmentEvent(Long userId, Long departmentId, Long deptRoleId) {
        this.shownData = new HashMap<>();
        this.userId = userId;
        this.shownData.put("userId", userId);
        this.departmentId = departmentId;
        this.shownData.put("departmentId", departmentId);
        if (deptRoleId != null) {
            this.role = RoleType.findById(deptRoleId);
            this.shownData.put("role", role.name());
        } else {
            this.role = null;
        }
    }

    @Override
    public Map<String, Object> getEventData() {
        return shownData;
    }
}
