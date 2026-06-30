package geumjeongyahak.domain.teacher_application.service.dto;

import geumjeongyahak.domain.teacher_application.v1.dto.response.TeacherApplicationResponse;
import java.util.List;

public record TeacherApplicationDetail(
    TeacherApplicationResponse application,
    String statusLabel,
    boolean pending,
    List<TeacherApplicationSubjectOption> subjectOptions
) {
}
