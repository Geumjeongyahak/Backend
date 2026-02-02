package sonmoeum.api.v1.students;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.ApiResponse;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.students.dto.request.CreateStudentRequest;
import sonmoeum.api.v1.students.dto.request.UpdateStudentRequest;
import sonmoeum.api.v1.students.dto.response.StudentResponse;
import sonmoeum.domain.student.service.StudentService;
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

@Tag(name = "Students", description = "학생 관리 API")
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_STUDENTS')")
    @Operation(summary = "학생 목록 조회", description = "페이지네이션된 학생 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<BasePageResponse<StudentResponse>> getStudents(BasePageRequest pageRequest) {
        return ApiResponse.success(studentService.getStudentPagination(pageRequest));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_STUDENTS')")
    @Operation(summary = "학생 상세 조회", description = "ID로 학생을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<StudentResponse> getStudent(@PathVariable Long id) {
        return ApiResponse.success(studentService.getStudentById(id));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_STUDENTS')")
    @Operation(summary = "학생 생성", description = "새로운 학생을 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return ApiResponse.success(studentService.createStudent(request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_STUDENTS')")
    @Operation(summary = "학생 수정", description = "학생 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        return ApiResponse.success(studentService.updateStudent(id, request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_STUDENTS')")
    @Operation(summary = "학생 삭제", description = "학생을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ApiResponse.success(null);
    }
}
