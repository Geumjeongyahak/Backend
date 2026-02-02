package sonmoeum.domain.subject.event;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record SubjectCreatedEvent(
    Long subjectId,
    Long teacherId,
    LocalDate startAt,
    LocalDate endAt,
    Integer times,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {}
