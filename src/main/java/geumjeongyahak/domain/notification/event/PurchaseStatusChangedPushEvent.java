package geumjeongyahak.domain.notification.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PurchaseStatusChangedPushEvent extends BaseEventDto {

    private final Long userId; // The recipient
    private final Long requestId;
    private final String newStatus;
    private final String title;
    private final String body;

    public PurchaseStatusChangedPushEvent(Long userId, Long requestId, String newStatus, String title, String body) {
        this.userId = userId;
        this.requestId = requestId;
        this.newStatus = newStatus;
        this.title = title;
        this.body = body;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("requestId", requestId);
        data.put("newStatus", newStatus);
        data.put("title", title);
        data.put("body", body);
        return data;
    }
}
