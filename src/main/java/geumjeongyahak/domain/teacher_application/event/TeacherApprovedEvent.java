package geumjeongyahak.domain.teacher_application.event;

import java.time.LocalDate;

public record TeacherApprovedEvent(
    Long userId,
    Long classroomId,
    LocalDate teacherStartAt,
    LocalDate teacherEndAt
) {
}
