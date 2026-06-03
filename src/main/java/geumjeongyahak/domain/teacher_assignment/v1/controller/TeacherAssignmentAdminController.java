package geumjeongyahak.domain.teacher_assignment.v1.controller;

import geumjeongyahak.domain.subject.v1.dto.response.SubjectDetailResponse;
import geumjeongyahak.domain.teacher_assignment.service.TeacherAssignmentService;
import geumjeongyahak.domain.teacher_assignment.v1.dto.request.AssignTeacherToScheduleRequest;
import geumjeongyahak.domain.teacher_assignment.v1.dto.request.UnassignTeacherScheduleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/teacher-schedule-assignments")
@RequiredArgsConstructor
@Tag(name = "Teacher Schedule Assignment Admin", description = "교사 시간표 배정 관리자 API")
public class TeacherAssignmentAdminController {

    private static final String MANAGE_PERMISSION = "hasRole('ADMIN') or hasAuthority('subject:manage:*')";

    private final TeacherAssignmentService teacherAssignmentService;

    @PreAuthorize(MANAGE_PERMISSION)
    @Operation(summary = "시간표 담당 교사 임의 배정")
    @PatchMapping
    public ResponseEntity<List<SubjectDetailResponse>> assignTeacher(
        @Valid @RequestBody AssignTeacherToScheduleRequest request
    ) {
        log.debug("PATCH /api/v1/admin/teacher-schedule-assignments - 교사 시간표 배정 요청");
        return ResponseEntity.ok(teacherAssignmentService.assignSchedule(
            request.subjectIds(),
            request.teacherId(),
            request.replacementConfirmed()
        ));
    }

    @PreAuthorize(MANAGE_PERMISSION)
    @Operation(summary = "시간표 담당 교사 해제")
    @DeleteMapping
    public ResponseEntity<List<SubjectDetailResponse>> unassignTeacher(
        @Valid @RequestBody UnassignTeacherScheduleRequest request
    ) {
        log.debug("DELETE /api/v1/admin/teacher-schedule-assignments - 교사 시간표 배정 해제 요청");
        return ResponseEntity.ok(teacherAssignmentService.unassignSchedule(request.subjectIds()));
    }
}
