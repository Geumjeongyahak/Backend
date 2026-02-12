package sonmoeum.domain.lesson.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import sonmoeum.common.security.service.CustomUserDetails;
import sonmoeum.domain.lesson.service.LessonService;
import sonmoeum.domain.lesson.service.StudentAttendanceService;
import sonmoeum.domain.lesson.v1.dto.request.LessonRangeRequest;
import sonmoeum.domain.lesson.v1.dto.request.UpdateLessonNoteRequest;
import sonmoeum.domain.lesson.v1.dto.request.UpdateLessonStatusRequest;
import sonmoeum.domain.lesson.v1.dto.request.UpdateStudentAttendancesRequest;
import sonmoeum.domain.lesson.v1.dto.request.UpdateTeacherAttendanceRequest;
import sonmoeum.domain.lesson.v1.dto.response.LessonDetailResponse;
import sonmoeum.domain.lesson.v1.dto.response.LessonNoteResponse;
import sonmoeum.domain.lesson.v1.dto.response.LessonSummaryResponse;
import sonmoeum.domain.lesson.v1.dto.response.StudentAttendanceResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@Tag(name = "Lesson", description = "수업 관리 API")
public class LessonController {

    private final LessonService lessonService;
    private final StudentAttendanceService studentAttendanceService;

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "전체 수업 조회", description = "전체 수업을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<LessonSummaryResponse>> getAllLessons(
        @ModelAttribute @Valid LessonRangeRequest request
    ) {
        log.debug("GET /api/v1/lessons - 전체 수업 목록 조회 요청");
        List<LessonSummaryResponse> response = lessonService.getAllLessons(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
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

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "수업 상세 조회", description = "수업 상세 정보를 조회합니다.")
    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonDetailResponse> getLessonDetail(
        @PathVariable Long lessonId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/{} - 수업 상세 조회 요청", lessonId);
        boolean isAdmin = userDetails.isAdmin();
        LessonDetailResponse response = lessonService.getLessonDetail(userDetails.getUserId(), lessonId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "학생 출석부 조회", description = "수업의 학생 출석부를 조회합니다.")
    @GetMapping("/{lessonId}/student-attendances")
    public ResponseEntity<List<StudentAttendanceResponse>> getStudentAttendances(
        @PathVariable Long lessonId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/{}/student-attendances - 학생 출석부 조회 요청", lessonId);
        boolean isAdmin = userDetails.isAdmin();
        List<StudentAttendanceResponse> response = studentAttendanceService.getStudentAttendances(
            userDetails.getUserId(),
            lessonId,
            isAdmin
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "수업 노트 조회", description = "수업 노트를 조회합니다.")
    @GetMapping("/{lessonId}/note")
    public ResponseEntity<LessonNoteResponse> getNote(
        @PathVariable Long lessonId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/{}/note - 수업 노트 조회 요청", lessonId);
        boolean isAdmin = userDetails.isAdmin();

        LessonNoteResponse response = lessonService.getNote(
            userDetails.getUserId(), lessonId, isAdmin
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "교사 출석 처리", description = "교사 출석 상태를 처리합니다.")
    @PatchMapping("/{lessonId}/teacher-attendance")
    public ResponseEntity<LessonDetailResponse> updateLessonAttendance(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateTeacherAttendanceRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lessons/{}/teacher-attendance - 교사 출석 처리 요청 (status={})",
            lessonId, request.status());
        boolean isAdmin = userDetails.isAdmin();
        LessonDetailResponse response = lessonService.updateTeacherAttendance(
            userDetails.getUserId(),
            lessonId,
            request.status(),
            isAdmin
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "학생 출석 처리", description = "학생 출석 상태를 처리합니다.")
    @PatchMapping("/{lessonId}/student-attendances")
    public ResponseEntity<List<StudentAttendanceResponse>> updateStudentAttendance(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateStudentAttendancesRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lessons/{}/student-attendances - 학생 출석 처리 요청", lessonId);
        boolean isAdmin = userDetails.isAdmin();
        List<StudentAttendanceResponse> response = studentAttendanceService.updateStudentAttendances(
            userDetails.getUserId(),
            lessonId,
            request,
            isAdmin
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "수업 상태 변경", description = "수업 상태를 변경합니다.")
    @PatchMapping("/{lessonId}/status")
    public ResponseEntity<LessonDetailResponse> updateLessonStatus(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateLessonStatusRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lessons/{}/status - 수업 상태 변경 요청", lessonId);
        boolean isAdmin = userDetails.isAdmin();
        LessonDetailResponse response = lessonService.updateLessonStatus(
            userDetails.getUserId(),
            lessonId,
            request.status(),
            isAdmin
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "수업 노트 업데이트", description = "수업 노트를 업데이트합니다.")
    @PutMapping("/{lessonId}/note")
    public ResponseEntity<LessonNoteResponse> upsertNote(
        @PathVariable Long lessonId,
        @Valid @RequestBody UpdateLessonNoteRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PUT /api/v1/lessons/{}/note - 수업 노트 업데이트 요청", lessonId);
        boolean isAdmin = userDetails.isAdmin();

        LessonNoteResponse response = lessonService.upsertNote(
            userDetails.getUserId(), lessonId, request.note(), isAdmin
        );
        return ResponseEntity.ok(response);
    }
}
