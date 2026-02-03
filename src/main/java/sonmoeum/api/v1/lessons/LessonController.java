package sonmoeum.api.v1.lessons;

import java.time.LocalDate;
import java.util.List;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.ApiResponse;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.lessons.dto.request.UpdateAttendanceRequest;
import sonmoeum.api.v1.lessons.dto.response.LessonResponse;
import sonmoeum.common.security.service.CustomUserDetails;
import sonmoeum.domain.lesson.service.LessonService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Lessons", description = "수업 관리 API")
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_LESSONS')")
    @Operation(summary = "수업 목록 조회", description = "페이지네이션된 수업 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<BasePageResponse<LessonResponse>> getLessons(BasePageRequest pageRequest) {
        return ApiResponse.success(lessonService.getLessonPagination(pageRequest));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_LESSONS')")
    @Operation(summary = "수업 상세 조회", description = "ID로 수업을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<LessonResponse> getLesson(@PathVariable Long id) {
        return ApiResponse.success(lessonService.getLessonById(id));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 수업 조회", description = "로그인된 사용자의 수업 목록을 기간별로 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<List<LessonResponse>> getMeLessons(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        if (userDetails == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return ApiResponse.success(lessonService.getMyLessons(userDetails.getUserId(), from, to));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_LESSONS')")
    @Operation(summary = "출석 상태 수정", description = "수업의 출석 상태를 수정합니다.")
    @PatchMapping("/{id}/attendance")
    public ApiResponse<LessonResponse> updateAttendance(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttendanceRequest request) {
        return ApiResponse.success(lessonService.updateAttendance(id, request.attendance()));
    }
}
