package geumjeongyahak.domain.lesson.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.domain.lesson.service.LessonService;
import geumjeongyahak.domain.lesson.v1.dto.request.LessonRangeRequest;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonSummaryResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@Tag(name = "Lesson", description = "수업 관리 API")
public class LessonController {

    private final LessonService lessonService;

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
}
