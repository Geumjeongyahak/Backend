package geumjeongyahak.domain.teacher_application.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.teacher_application.service.TeacherApplicationService;
import geumjeongyahak.domain.teacher_application.v1.dto.request.CreateTeacherApplicationRequest;
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
}
