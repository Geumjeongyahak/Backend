package geumjeongyahak.domain.lesson.v1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.lesson.service.LessonService;
import geumjeongyahak.domain.lesson.v1.dto.request.CreateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@Tag(name = "Lesson", description = "수업 관리 API")
public class LessonAdminController {
    private final LessonService lessonService;

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('lesson:manage:*')")
    @Operation(summary = "수업 생성", description = "수업을 생성합니다.")
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

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('lesson:manage:*')")
    @Operation(summary = "수업 수정", description = "수업을 수정합니다.")
    @PatchMapping("/{lessonId}")
    public ResponseEntity<LessonDetailResponse> updateLesson(
        @PathVariable Long lessonId,
        @RequestBody UpdateLessonRequest request
    ) {
        log.debug("PATCH /api/v1/lessons/{} - 수업 수정 요청", lessonId);
        LessonDetailResponse response = lessonService.updateLesson(lessonId, request);
        return ResponseEntity.ok(response);
    }
  
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('lesson:manage:*')")
    @Operation(summary = "수업 삭제", description = "수업을 삭제합니다.")
    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long lessonId) {
        log.debug("DELETE /api/v1/lessons/{} - 수업 삭제 요청", lessonId);
        lessonService.deleteLesson(lessonId);

        return ResponseEntity.noContent().build();
    }
}