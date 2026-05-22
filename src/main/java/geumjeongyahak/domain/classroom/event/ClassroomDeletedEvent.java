package geumjeongyahak.domain.classroom.event;

import java.util.Map;

import geumjeongyahak.common.event.dto.BaseEventDto;

public class ClassroomDeletedEvent extends BaseEventDto {

    private final Long classroomId;

    public ClassroomDeletedEvent(Long classroomId) {
        this.classroomId = classroomId;
    }

    public Long classroomId() {
        return classroomId;
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of("classroomId", classroomId);
    }
}
