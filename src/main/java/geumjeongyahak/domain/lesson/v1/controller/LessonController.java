package geumjeongyahak.domain.lesson.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.lesson.service.LessonService;
import geumjeongyahak.domain.lesson.service.StudentAttendanceService;
import geumjeongyahak.domain.lesson.v1.dto.request.CreateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.LessonRangeRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateLessonNoteRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateLessonStatusRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateStudentAttendancesRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateTeacherAttendanceRequest;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonDetailResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonNoteResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonSummaryResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.StudentAttendanceResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@Tag(name = "Lesson", description = "수업 관리 API")
public class LessonController {

    private static final String TEACHER_OR_HIGHER_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')";
    private static final String MANAGER_OR_HIGHER_ACCESS =
        "hasRole('MANAGER') or hasRole('ADMIN')";

    private final LessonService lessonService;
    private final StudentAttendanceService studentAttendanceService;

    @PreAuthorize(MANAGER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 생성",
        description = "ADMIN 또는 MANAGER 가 수업을 생성합니다. "
            + "담당 교사는 VOLUNTEER 역할 사용자만 지정할 수 있습니다. "
            + "같은 교사의 같은 날짜 수업 시간이 기존 수업과 겹치면 생성할 수 없습니다. "
            + "생성된 수업은 SCHEDULED 상태와 기본 교사 출석 상태로 저장됩니다."
    )
    @PostMapping
    public ResponseEntity<LessonDetailResponse> createLesson(
        @Valid @RequestBody CreateLessonRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/lessons - 수업 생성 요청 (subjectId={}, teacherId={}, date={}, period={})",
            request.subjectId(), request.teacherId(), request.date(), request.period());
        LessonDetailResponse response = lessonService.createLesson(
            userDetails.getUserId(),
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "전체 수업 조회",
        description = "인증된 사용자가 기간 조건에 해당하는 전체 수업 목록을 조회합니다. "
            + "수업 목록은 날짜와 교시 기준으로 정렬되어 반환됩니다. "
            + "조회 API는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping
    public ResponseEntity<List<LessonSummaryResponse>> getAllLessons(
        @ModelAttribute @Valid LessonRangeRequest request
    ) {
        log.debug("GET /api/v1/lessons - 전체 수업 목록 조회 요청");
        List<LessonSummaryResponse> response = lessonService.getAllLessons(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "내 수업 목록 조회",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 본인이 담당하는 수업 목록을 조회합니다. "
            + "기간 조건에 해당하는 수업만 반환되며, 날짜와 교시 기준으로 정렬됩니다. "
            + "조회 API는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping("/me")
    public ResponseEntity<List<LessonSummaryResponse>> getMyLessons(
        @ModelAttribute @Valid LessonRangeRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/my - 내 수업 목록 조회 요청");
        List<LessonSummaryResponse> responses = lessonService.getMyLessons(userDetails.getUserId(), request);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 상세 조회",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 수업 상세 정보를 조회합니다. "
            + "ADMIN 또는 MANAGER 는 모든 수업을 조회할 수 있고, VOLUNTEER 는 본인이 담당하는 수업만 조회할 수 있습니다. "
            + "응답에는 날짜, 교시, 시간, 수업 상태, 교사 출석 상태, 담당 교사, 과목, 노트가 포함됩니다. "
            + "조회 API는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonDetailResponse> getLessonDetail(
        @PathVariable Long lessonId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/{} - 수업 상세 조회 요청", lessonId);
        boolean canAccessAnyLesson = userDetails.isAdminOrManager();
        LessonDetailResponse response = lessonService.getLessonDetail(
            userDetails.getUserId(),
            lessonId,
            canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "학생 출석부 조회",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 수업의 학생 출석부를 조회합니다. "
            + "ADMIN 또는 MANAGER 는 모든 수업의 학생 출석부를 조회할 수 있고, VOLUNTEER 는 본인이 담당하는 수업만 조회할 수 있습니다. "
            + "조회 API는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping("/{lessonId}/student-attendances")
    public ResponseEntity<List<StudentAttendanceResponse>> getStudentAttendances(
        @PathVariable Long lessonId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/{}/student-attendances - 학생 출석부 조회 요청", lessonId);
        boolean canAccessAnyLesson = userDetails.isAdminOrManager();
        List<StudentAttendanceResponse> response = studentAttendanceService.getStudentAttendances(
            userDetails.getUserId(),
            lessonId,
            canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 노트 조회",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 수업 노트를 조회합니다. "
            + "ADMIN 또는 MANAGER 는 모든 수업 노트를 조회할 수 있고, VOLUNTEER 는 본인이 담당하는 수업 노트만 조회할 수 있습니다. "
            + "조회 API는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping("/{lessonId}/note")
    public ResponseEntity<LessonNoteResponse> getNote(
        @PathVariable Long lessonId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/{}/note - 수업 노트 조회 요청", lessonId);
        boolean canAccessAnyLesson = userDetails.isAdminOrManager();

        LessonNoteResponse response = lessonService.getNote(
            userDetails.getUserId(), lessonId, canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(MANAGER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 수정",
        description = "ADMIN 또는 MANAGER 가 수업 정보를 부분 수정합니다. "
            + "과목, 담당 교사, 날짜, 시작 시각, 종료 시각, 교시를 변경할 수 있습니다. "
            + "담당 교사를 변경하는 경우 새 담당 교사는 VOLUNTEER 역할 사용자여야 합니다. "
            + "수정 후 같은 교사의 같은 날짜 수업 시간이 다른 수업과 겹치면 수정할 수 없습니다."
    )
    @PatchMapping("/{lessonId}")
    public ResponseEntity<LessonDetailResponse> updateLesson(
        @PathVariable Long lessonId,
        @RequestBody UpdateLessonRequest request
    ) {
        log.debug("PATCH /api/v1/lessons/{} - 수업 수정 요청", lessonId);
        LessonDetailResponse response = lessonService.updateLesson(lessonId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "교사 출석 처리",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 수업의 교사 출석 상태를 변경합니다. "
            + "ADMIN 또는 MANAGER 는 모든 수업의 교사 출석을 처리할 수 있고, VOLUNTEER 는 본인이 담당하는 수업만 처리할 수 있습니다. "
            + "요청한 교사 출석 상태가 수업에 저장됩니다."
    )
    @PatchMapping("/{lessonId}/teacher-attendance")
    public ResponseEntity<LessonDetailResponse> updateLessonAttendance(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateTeacherAttendanceRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lessons/{}/teacher-attendance - 교사 출석 처리 요청 (status={})",
            lessonId, request.status());
        boolean canAccessAnyLesson = userDetails.isAdminOrManager();
        LessonDetailResponse response = lessonService.updateTeacherAttendance(
            userDetails.getUserId(),
            lessonId,
            request.status(),
            canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "학생 출석 처리",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 수업의 학생 출석 상태를 일괄 변경합니다. "
            + "ADMIN 또는 MANAGER 는 모든 수업의 학생 출석을 처리할 수 있고, VOLUNTEER 는 본인이 담당하는 수업만 처리할 수 있습니다. "
            + "요청에 포함된 학생별 출석 상태와 메모가 저장됩니다."
    )
    @PatchMapping("/{lessonId}/student-attendances")
    public ResponseEntity<List<StudentAttendanceResponse>> updateStudentAttendance(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateStudentAttendancesRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lessons/{}/student-attendances - 학생 출석 처리 요청", lessonId);
        boolean canAccessAnyLesson = userDetails.isAdminOrManager();
        List<StudentAttendanceResponse> response = studentAttendanceService.updateStudentAttendances(
            userDetails.getUserId(),
            lessonId,
            request,
            canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 상태 변경",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 수업 상태를 변경합니다. "
            + "ADMIN 또는 MANAGER 는 모든 수업 상태를 변경할 수 있고, VOLUNTEER 는 본인이 담당하는 수업만 변경할 수 있습니다. "
            + "요청한 수업 상태가 저장됩니다."
    )
    @PatchMapping("/{lessonId}/status")
    public ResponseEntity<LessonDetailResponse> updateLessonStatus(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateLessonStatusRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lessons/{}/status - 수업 상태 변경 요청", lessonId);
        boolean canAccessAnyLesson = userDetails.isAdminOrManager();
        LessonDetailResponse response = lessonService.updateLessonStatus(
            userDetails.getUserId(),
            lessonId,
            request.status(),
            canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 노트 업데이트",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 수업 노트를 생성하거나 수정합니다. "
            + "ADMIN 또는 MANAGER 는 모든 수업 노트를 수정할 수 있고, VOLUNTEER 는 본인이 담당하는 수업 노트만 수정할 수 있습니다. "
            + "요청한 노트 내용이 수업에 저장됩니다."
    )
    @PutMapping("/{lessonId}/note")
    public ResponseEntity<LessonNoteResponse> upsertNote(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateLessonNoteRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PUT /api/v1/lessons/{}/note - 수업 노트 업데이트 요청", lessonId);
        boolean canAccessAnyLesson = userDetails.isAdminOrManager();

        LessonNoteResponse response = lessonService.upsertNote(
            userDetails.getUserId(), lessonId, request.note(), canAccessAnyLesson
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(MANAGER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 삭제",
        description = "ADMIN 또는 MANAGER 가 수업을 삭제합니다. "
            + "삭제는 소프트 삭제로 처리되며, 삭제된 수업은 일반 조회 대상에서 제외됩니다."
    )
    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long lessonId) {
        log.debug("DELETE /api/v1/lessons/{} - 수업 삭제 요청", lessonId);
        lessonService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }
}
