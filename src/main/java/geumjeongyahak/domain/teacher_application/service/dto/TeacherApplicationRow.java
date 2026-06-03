package geumjeongyahak.domain.teacher_application.service.dto;

import geumjeongyahak.domain.teacher_application.v1.dto.response.TeacherApplicationListResponse;

public record TeacherApplicationRow(
    TeacherApplicationListResponse application,
    String statusLabel
) {
}
