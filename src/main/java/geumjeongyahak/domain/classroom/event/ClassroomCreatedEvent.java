package geumjeongyahak.domain.classroom.event;

import java.util.Map;

import geumjeongyahak.common.event.dto.BaseEventDto;

public class ClassroomCreatedEvent extends BaseEventDto {

    private final Long classroomId;
    private final String classroomName;

    public ClassroomCreatedEvent(Long classroomId, String classroomName) {
        this.classroomId = classroomId;
        this.classroomName = classroomName;
    }

    public Long classroomId() {
        return classroomId;
    }

    public String classroomName() {
        return classroomName;
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of("classroomId", classroomId, "classroomName", classroomName);
    }
}
