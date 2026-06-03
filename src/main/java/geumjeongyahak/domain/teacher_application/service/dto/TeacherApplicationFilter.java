package geumjeongyahak.domain.teacher_application.service.dto;

import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;

public record TeacherApplicationFilter(
    TeacherApplicationStatus status,
    String keyword,
    Integer page,
    Integer size
) {
}
