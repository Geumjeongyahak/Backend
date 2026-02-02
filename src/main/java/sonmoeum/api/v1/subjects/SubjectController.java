package sonmoeum.api.v1.subjects;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.ApiResponse;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.subjects.dto.request.CreateSubjectRequest;
import sonmoeum.api.v1.subjects.dto.request.UpdateSubjectRequest;
import sonmoeum.api.v1.subjects.dto.response.SubjectResponse;
import sonmoeum.domain.subject.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Subjects", description = "과목 관리 API")
@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_SUBJECTS')")
    @Operation(summary = "과목 목록 조회", description = "페이지네이션된 과목 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<BasePageResponse<SubjectResponse>> getSubjects(BasePageRequest pageRequest) {
        return ApiResponse.success(subjectService.getSubjectPagination(pageRequest));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_SUBJECTS')")
    @Operation(summary = "과목 상세 조회", description = "ID로 과목을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<SubjectResponse> getSubject(@PathVariable Long id) {
        return ApiResponse.success(subjectService.getSubjectById(id));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_SUBJECTS')")
    @Operation(summary = "과목 생성", description = "새로운 과목을 생성합니다. 과목 생성 시 수업이 자동으로 생성됩니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubjectResponse> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        return ApiResponse.success(subjectService.createSubject(request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_SUBJECTS')")
    @Operation(summary = "과목 수정", description = "과목 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<SubjectResponse> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubjectRequest request) {
        return ApiResponse.success(subjectService.updateSubject(id, request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_SUBJECTS')")
    @Operation(summary = "과목 삭제", description = "과목을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ApiResponse.success(null);
    }
}
