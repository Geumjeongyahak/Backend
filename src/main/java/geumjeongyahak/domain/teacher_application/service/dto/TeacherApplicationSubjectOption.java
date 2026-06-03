package geumjeongyahak.domain.teacher_application.service.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TeacherApplicationSubjectOption(
    Long id,
    String name,
    String classroomName,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {
}
