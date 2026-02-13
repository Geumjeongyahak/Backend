package sonmoeum.domain.subject.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sonmoeum.domain.subject.service.SubjectService;
import sonmoeum.domain.subject.v1.dto.request.CreateSubjectRequest;
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
}
