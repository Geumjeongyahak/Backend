package geumjeongyahak.domain.teacher_application.service.dto;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import java.util.List;

public record TeacherApplicationPage(
    AdminPage<TeacherApplicationRow> applications,
    TeacherApplicationFilter filter,
    List<TeacherApplicationStatusOption> statusOptions
) {
}
