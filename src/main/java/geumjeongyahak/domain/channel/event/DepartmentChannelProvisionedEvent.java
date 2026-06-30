package geumjeongyahak.domain.channel.event;

import geumjeongyahak.common.event.dto.BaseEventDto;

import java.util.Map;

public class DepartmentChannelProvisionedEvent extends BaseEventDto {

    private final Long departmentId;
    private final Long channelId;

    public DepartmentChannelProvisionedEvent(Long departmentId, Long channelId) {
        this.departmentId = departmentId;
        this.channelId = channelId;
    }

    public Long departmentId() {
        return departmentId;
    }

    public Long channelId() {
        return channelId;
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of("departmentId", departmentId, "channelId", channelId);
    }
}
