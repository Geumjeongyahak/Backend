package geumjeongyahak.domain.teacher_application.v1.controller;

import geumjeongyahak.domain.teacher_application.service.TeacherApplicationService;
import geumjeongyahak.domain.teacher_application.v1.dto.response.TeacherApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/teacher-applications")
@RequiredArgsConstructor
@Tag(name = "TeacherApplication Admin", description = "교원 신청 관리자 API")
public class TeacherApplicationAdminController {

    private static final String READ_PERMISSION = "hasRole('ADMIN') or hasAuthority('teacher-application:read:*')";

    private final TeacherApplicationService teacherApplicationService;

    @PreAuthorize(READ_PERMISSION)
    @Operation(summary = "교원 신청 상세 조회")
    @GetMapping("/{applicationId}")
    public ResponseEntity<TeacherApplicationResponse> getTeacherApplication(
        @PathVariable Long applicationId
    ) {
        log.debug("GET /api/v1/admin/teacher-applications/{}", applicationId);
        return ResponseEntity.ok(teacherApplicationService.getTeacherApplication(applicationId));
    }
}
