package geumjeongyahak.domain.teacher_application.service.dto;

import geumjeongyahak.domain.subject.entity.Subject;
import java.time.DayOfWeek;
import java.time.LocalDate;

public record ScheduleGroupKey(
    Long classroomId,
    DayOfWeek dayOfWeek,
    LocalDate startAt,
    LocalDate endAt
) {
    public static ScheduleGroupKey from(Subject subject) {
        return new ScheduleGroupKey(
            subject.getClassroom().getId(),
            subject.getDayOfWeek(),
            subject.getStartAt(),
            subject.getEndAt()
        );
    }

    public String value() {
        return classroomId + ":" + dayOfWeek + ":" + startAt + ":" + endAt;
    }
}
