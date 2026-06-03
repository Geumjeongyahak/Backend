package geumjeongyahak.domain.teacher_application.service.dto;

import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;

public record TeacherApplicationStatusOption(
    TeacherApplicationStatus status,
    String label
) {
}
