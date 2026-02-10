package sonmoeum.domain.student.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sonmoeum.domain.base.dto.response.PaginationResponse;
import sonmoeum.domain.student.service.StudentService;
import sonmoeum.domain.student.v1.dto.request.CreateStudentRequest;
import sonmoeum.domain.student.v1.dto.request.StudentPaginationRequest;
import sonmoeum.domain.student.v1.dto.response.StudentResponse;
import sonmoeum.domain.users.v1.dto.request.UserPaginationRequest;
import sonmoeum.domain.users.v1.dto.response.UserResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Student", description = "학생 관리 API")
public class StudentController {

    private final StudentService studentService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "학생 등록", description = "새로운 학생을 등록합니다.")
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
        @Valid @RequestBody CreateStudentRequest request
    ) {
        log.debug("POST /api/v1/students - 학생 생성 요청: {}", request);
        StudentResponse response = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "학생 목록 조회", description = "전체 학생 목록을 페이지네이션하여 조회합니다.")
    @GetMapping
    public ResponseEntity<PaginationResponse<StudentResponse>> getAllStudents(
        @ParameterObject @Valid StudentPaginationRequest request
    ) {
        log.debug("GET /api/v1/users - 학생 목록 조회 요청");
        PaginationResponse<StudentResponse> response = studentService.getAllStudents(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "학생 단건 조회", description = "ID로 특정 학생을 조회합니다.")
    @GetMapping("/{studentId}")
    public ResponseEntity<StudentResponse> getStudentById(
        @Parameter(description = "학생 식별자", example = "1")
        @PathVariable Long studentId
    ) {
        log.debug("GET /api/v1/students/{} - 학생 단건 조회 요청", studentId);
        StudentResponse response = studentService.getStudentById(studentId);
        return ResponseEntity.ok(response);
    }
}
