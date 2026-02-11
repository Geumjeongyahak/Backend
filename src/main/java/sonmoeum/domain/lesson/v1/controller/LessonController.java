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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sonmoeum.common.security.service.CustomUserDetails;
import sonmoeum.domain.lesson.service.LessonService;
import sonmoeum.domain.lesson.v1.dto.request.LessonRangeRequest;
import sonmoeum.domain.lesson.v1.dto.request.UpdateTeacherAttendanceRequest;
import sonmoeum.domain.lesson.v1.dto.response.LessonDetailResponse;
import sonmoeum.domain.lesson.v1.dto.response.LessonSummaryResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@Tag(name = "Lesson", description = "수업 관리 API")
public class LessonController {

    private final LessonService lessonService;

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
    @GetMapping("/my")
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
        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        LessonDetailResponse response = lessonService.getLessonDetail(userDetails.getUserId(), lessonId, isAdmin);
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
        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        LessonDetailResponse response = lessonService.updateTeacherAttendance(
            userDetails.getUserId(),
            lessonId,
            request.status(),
            isAdmin
        );
        return ResponseEntity.ok(response);
    }
}
