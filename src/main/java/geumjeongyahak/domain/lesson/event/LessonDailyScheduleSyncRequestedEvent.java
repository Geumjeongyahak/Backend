package geumjeongyahak.domain.lesson.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class LessonDailyScheduleSyncRequestedEvent extends BaseEventDto {

    private final Long classroomId;
    private final LocalDate lessonDate;

    public LessonDailyScheduleSyncRequestedEvent(Long classroomId, LocalDate lessonDate) {
        this.classroomId = classroomId;
        this.lessonDate = lessonDate;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("classroomId", classroomId);
        data.put("lessonDate", lessonDate);
        return data;
    }
}
