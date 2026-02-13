package sonmoeum.domain.subject.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sonmoeum.domain.subject.service.SubjectService;
import sonmoeum.domain.subject.v1.dto.request.CreateSubjectRequest;
import sonmoeum.domain.subject.v1.dto.request.UpdateSubjectRequest;
import sonmoeum.domain.subject.v1.dto.response.SubjectDetailResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Tag(name = "Subject", description = "과목 관리 API")
public class SubjectController {

    private final SubjectService subjectService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "과목 등록", description = "새로운 과목을 등록합니다.")
    @PostMapping
    public ResponseEntity<SubjectDetailResponse> createSubject(
        @RequestBody @Valid CreateSubjectRequest request
    ) {
        log.debug("POST /api/v1/subjects - 과목 등록 요청");
        SubjectDetailResponse response = subjectService.createSubject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "과목 단건 조회", description = "과목 ID로 과목 정보를 조회합니다.")
    @GetMapping("/{subjectId}")
    public ResponseEntity<SubjectDetailResponse> getSubject(@PathVariable Long subjectId) {
        log.debug("GET /api/v1/subjects/{} - 과목 단건 조회 요청", subjectId);
        SubjectDetailResponse response = subjectService.getSubject(subjectId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "과목 목록 조회", description = "과목 목록을 조회합니다. classroomId가 있으면 해당 분반의 과목만 조회합니다.")
    @GetMapping
    public ResponseEntity<List<SubjectDetailResponse>> getAllSubjects(
        @RequestParam(required = false) Long classroomId
    ) {
        log.debug("GET /api/v1/subjects - 과목 목록 조회 요청 (classroomId={})", classroomId);
        return ResponseEntity.ok(subjectService.getAllSubjects(classroomId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "과목 부분 수정", description = "과목 정보를 부분 수정합니다.")
    @PatchMapping("/{subjectId}")
    public ResponseEntity<SubjectDetailResponse> updateSubject(
        @PathVariable Long subjectId,
        @Valid @RequestBody UpdateSubjectRequest request
    ) {
        log.debug("PATCH /api/v1/subjects/{} - 과목 부분 수정 요청", subjectId);
        SubjectDetailResponse response = subjectService.updateSubject(subjectId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "과목 삭제", description = "과목을 삭제(비활성화)합니다.")
    @DeleteMapping("/{subjectId}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long subjectId) {
        log.debug("DELETE /api/v1/subjects/{} - 과목 삭제(비활성화) 요청", subjectId);
        subjectService.deleteSubject(subjectId);
        return ResponseEntity.noContent().build();
    }
}
