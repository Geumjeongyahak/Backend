package geumjeongyahak.domain.teacher_application.service.dto;

import geumjeongyahak.domain.teacher_application.v1.dto.response.TeacherApplicationResponse;

public record TeacherApplicationRow(
    TeacherApplicationResponse application,
    String statusLabel
) {
}
