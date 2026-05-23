package geumjeongyahak.domain.teacher_application.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.teacher_application.service.TeacherApplicationService;
import geumjeongyahak.domain.teacher_application.v1.dto.request.CreateTeacherApplicationRequest;
import geumjeongyahak.domain.teacher_application.v1.dto.request.UpdateTeacherApplicationRequest;
import geumjeongyahak.domain.teacher_application.v1.dto.response.MyTeacherApplicationResponse;
import geumjeongyahak.domain.teacher_application.v1.dto.response.TeacherApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/teacher-applications")
@RequiredArgsConstructor
@Tag(name = "TeacherApplication", description = "교원 신청 API")
public class TeacherApplicationController {

    private final TeacherApplicationService teacherApplicationService;

    @PreAuthorize("hasRole('GUEST')")
    @Operation(
        summary = "교원 신청서 제출",
        description = "인증된 GUEST 사용자가 교원 신청서를 제출합니다. 같은 사용자는 PENDING 신청을 1개만 가질 수 있습니다."
    )
    @PostMapping
    public ResponseEntity<TeacherApplicationResponse> createTeacherApplication(
        @Valid @RequestBody CreateTeacherApplicationRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/teacher-applications (preferredSubjectId={})", request.preferredSubjectId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(teacherApplicationService.createTeacherApplication(userDetails.getUserId(), request));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "내 교원 신청 조회",
        description = "현재 로그인 사용자의 최신 PENDING, APPROVED, REJECTED 교원 신청 1건을 조회합니다. 없으면 exists=false를 반환합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<MyTeacherApplicationResponse> getMyTeacherApplication(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/teacher-applications/me");
        return ResponseEntity.ok(teacherApplicationService.getMyTeacherApplication(userDetails.getUserId()));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "교원 신청 수정",
        description = "신청자 본인이 PENDING 상태의 교원 신청서를 수정합니다."
    )
    @PatchMapping("/{applicationId}")
    public ResponseEntity<TeacherApplicationResponse> updateTeacherApplication(
        @PathVariable Long applicationId,
        @Valid @RequestBody UpdateTeacherApplicationRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/teacher-applications/{}", applicationId);
        return ResponseEntity.ok(
            teacherApplicationService.updateTeacherApplication(userDetails.getUserId(), applicationId, request)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "교원 신청 취소",
        description = "신청자 본인이 PENDING 상태의 교원 신청서를 취소합니다. 실제 삭제가 아니라 CANCELLED 상태로 변경합니다."
    )
    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> cancelTeacherApplication(
        @PathVariable Long applicationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/teacher-applications/{}", applicationId);
        teacherApplicationService.cancelTeacherApplication(userDetails.getUserId(), applicationId);
        return ResponseEntity.noContent().build();
    }
}
