package geumjeongyahak.common.event.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public abstract class BaseEventDto {
    private final LocalDateTime occurredAt;

    protected BaseEventDto() {
        this.occurredAt = LocalDateTime.now();
    }

    abstract public Map<String, Object> getEventData();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName())
          .append(" [ occurredAt=").append(occurredAt);
        for  (Map.Entry<String, Object> entry : getEventData().entrySet()) {
            sb.append(", ").append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append(" ]");
        return sb.toString();
    }
}
