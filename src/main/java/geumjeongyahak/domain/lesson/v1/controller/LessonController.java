package geumjeongyahak.domain.lesson.v1.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.common.security.service.DomainPermissionChecker;
import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.lesson.service.LessonService;
import geumjeongyahak.domain.lesson.v1.dto.request.LessonRangeRequest;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonDetailResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonSummaryResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@Tag(name = "Lesson", description = "수업 관리 API")
public class LessonController {
    private static final String TEACHER_OR_LESSON_READ_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN') or hasAuthority('lesson:read:*')";

    private final LessonService lessonService;
    private final DomainPermissionChecker domainPermissionChecker;

    @Operation(
        summary = "전체 수업 조회",
        description = """
            기간 조건에 해당하는 전체 수업 목록을 날짜와 교시 기준으로 정렬하여 반환합니다.

            시간표 표시 정보:
            - isExchanged: 교환형 또는 대체형 제안 수락으로 담당 교사가 변경된 수업인지 여부
            - isAbsent: 결석 요청 승인으로 결강 처리된 수업인지 여부
            - exchangedLessonDate: 교환형 수업의 상대 수업 날짜

            대체형 수업과 일반 수업은 exchangedLessonDate가 null입니다.
            같은 분반과 날짜의 모든 교시는 동일한 교환·결강 정보를 반환합니다.
            조회 API는 side effect를 발생시키지 않습니다.
            """
    )
    @GetMapping
    public ResponseEntity<List<LessonSummaryResponse>> getAllLessons(
        @ModelAttribute @Valid LessonRangeRequest request
    ) {
        log.debug("GET /api/v1/lessons - 전체 수업 목록 조회 요청");
        List<LessonSummaryResponse> response = lessonService.getAllLessons(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_LESSON_READ_ACCESS)
    @Operation(
        summary = "내 수업 목록 조회",
        description = """
            로그인 사용자가 담당하는 기간 내 수업 목록을 조회합니다.

            응답의 isExchanged와 isAbsent로 교환·결강 여부를 확인할 수 있습니다.
            교환형 수업은 exchangedLessonDate에 상대 수업 날짜가 반환되며,
            대체형 또는 일반 수업은 null이 반환됩니다.
            """
    )
    @GetMapping("/me")
    public ResponseEntity<List<LessonSummaryResponse>> getMyLessons(
        @ModelAttribute @Valid LessonRangeRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lessons/me - 내 수업 목록 조회 요청");
        List<LessonSummaryResponse> responses = lessonService.getMyLessons(userDetails.getUserId(), request);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize(TEACHER_OR_LESSON_READ_ACCESS)
    @Operation(
        summary = "수업 상세 조회",
        description = """
            개별 교시 수업의 상세 정보와 연결된 하루 일정 식별자를 조회합니다.

            연결된 DailySchedule을 기준으로 다음 시간표 표시 정보를 함께 반환합니다.
            - isExchanged: 교환 또는 대체 수업 여부
            - isAbsent: 승인된 결석 요청에 따른 결강 여부
            - exchangedLessonDate: 교환형 수업의 상대 날짜

            연결된 DailySchedule이 아직 없으면 false, false, null을 반환합니다.
            """
    )
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

    private boolean canReadAnyLesson(CustomUserDetails userDetails) {
        return userDetails.isAdminOrManager()
            || domainPermissionChecker.hasPermission(userDetails, ResourceType.LESSON, ActionType.READ, null);
    }
}
