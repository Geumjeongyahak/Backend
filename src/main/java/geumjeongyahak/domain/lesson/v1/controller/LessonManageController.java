package geumjeongyahak.domain.lesson.v1.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.common.security.service.DomainPermissionChecker;
import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.lesson.service.LessonService;
import geumjeongyahak.domain.lesson.service.StudentAttendanceService;
import geumjeongyahak.domain.lesson.v1.dto.request.LessonRangeRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateLessonNoteRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateStudentAttendancesRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateTeacherAttendanceRequest;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonDetailResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonNoteResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonSummaryResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.StudentAttendanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonManageController {
    private static final String TEACHER_OR_LESSON_READ_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN') or hasAuthority('lesson:read:*')";
    private static final String ASSIGNABLE_TEACHER_OR_LESSON_MANAGE_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN') or hasAuthority('lesson:manage:*')";

    private final LessonService lessonService;
    private final StudentAttendanceService studentAttendanceService;
    private final DomainPermissionChecker domainPermissionChecker;

    @PreAuthorize(TEACHER_OR_LESSON_READ_ACCESS)
    @Operation(summary = "내 수업 목록 조회", description = "내 수업 목록을 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<List<LessonSummaryResponse>> getMyLessons(
        @ModelAttribute @Valid LessonRangeRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/my - 내 수업 목록 조회 요청");
        List<LessonSummaryResponse> responses = lessonService.getMyLessons(userDetails.getUserId(), request);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize(TEACHER_OR_LESSON_READ_ACCESS)
    @Operation(summary = "수업 상세 조회", description = "수업 상세 정보를 조회합니다.")
    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonDetailResponse> getLessonDetail(
        @PathVariable Long lessonId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/{} - 수업 상세 조회 요청", lessonId);
        boolean canAccessAnyLesson = canReadAnyLesson(userDetails);
        LessonDetailResponse response = lessonService.getLessonDetail(
            userDetails.getUserId(),
            lessonId,
            canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_LESSON_READ_ACCESS)
    @Operation(summary = "학생 출석부 조회", description = "수업의 학생 출석부를 조회합니다.")
    @GetMapping("/{lessonId}/student-attendances")
    public ResponseEntity<List<StudentAttendanceResponse>> getStudentAttendances(
        @PathVariable Long lessonId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/{}/student-attendances - 학생 출석부 조회 요청", lessonId);
        boolean canAccessAnyLesson = canReadAnyLesson(userDetails);
        List<StudentAttendanceResponse> response = studentAttendanceService.getStudentAttendances(
            userDetails.getUserId(),
            lessonId,
            canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_LESSON_READ_ACCESS)
    @Operation(summary = "수업 노트 조회", description = "수업 노트를 조회합니다.")
    @GetMapping("/{lessonId}/note")
    public ResponseEntity<LessonNoteResponse> getNote(
        @PathVariable Long lessonId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/{}/note - 수업 노트 조회 요청", lessonId);
        boolean canAccessAnyLesson = canReadAnyLesson(userDetails);

        LessonNoteResponse response = lessonService.getNote(
            userDetails.getUserId(), lessonId, canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(ASSIGNABLE_TEACHER_OR_LESSON_MANAGE_ACCESS)
    @Operation(summary = "교사 출석 처리", description = "교사 출석 상태를 처리합니다.")
    @PatchMapping("/{lessonId}/teacher-attendance")
    public ResponseEntity<LessonDetailResponse> updateLessonAttendance(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateTeacherAttendanceRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lessons/{}/teacher-attendance - 교사 출석 처리 요청 (status={})",
            lessonId, request.status());
        boolean canAccessAnyLesson = canManageAnyLesson(userDetails);
        LessonDetailResponse response = lessonService.updateTeacherAttendance(
            userDetails.getUserId(),
            lessonId,
            request.status(),
            canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(ASSIGNABLE_TEACHER_OR_LESSON_MANAGE_ACCESS)
    @Operation(summary = "학생 출석 처리", description = "학생 출석 상태를 처리합니다.")
    @PatchMapping("/{lessonId}/student-attendances")
    public ResponseEntity<List<StudentAttendanceResponse>> updateStudentAttendance(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateStudentAttendancesRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lessons/{}/student-attendances - 학생 출석 처리 요청", lessonId);
        boolean canAccessAnyLesson = canManageAnyLesson(userDetails);
        List<StudentAttendanceResponse> response = studentAttendanceService.updateStudentAttendances(
            userDetails.getUserId(),
            lessonId,
            request,
            canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(ASSIGNABLE_TEACHER_OR_LESSON_MANAGE_ACCESS)
    @Operation(summary = "수업 노트 업데이트", description = "수업 노트를 업데이트합니다.")
    @PutMapping("/{lessonId}/note")
    public ResponseEntity<LessonNoteResponse> upsertNote(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateLessonNoteRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PUT /api/v1/lessons/{}/note - 수업 노트 업데이트 요청", lessonId);
        boolean canAccessAnyLesson = canManageAnyLesson(userDetails);

        LessonNoteResponse response = lessonService.upsertNote(
            userDetails.getUserId(), lessonId, request.note(), canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    private boolean canReadAnyLesson(CustomUserDetails userDetails) {
        return userDetails.isAdminOrManager()
            || domainPermissionChecker.hasPermission(userDetails, ResourceType.LESSON, ActionType.READ, null);
    }

    private boolean canManageAnyLesson(CustomUserDetails userDetails) {
        return domainPermissionChecker.hasPermission(userDetails, ResourceType.LESSON, ActionType.MANAGE, null);
    }
}
