package geumjeongyahak.domain.student.v1.controller;

import geumjeongyahak.domain.student.service.StudentService;
import geumjeongyahak.domain.student.v1.dto.request.CreateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.request.UpdateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(
    name = "Student Admin",
    description = """
        학생 등록, 수정, 삭제를 담당하는 관리자용 API입니다.
        학생 인적 사항 관리와 상태(재학/졸업/휴학 등) 관리를 수행할 때 사용합니다.
        단순 조회는 Student API가 담당합니다.
        """
)
public class StudentAdminController {

    private static final String STUDENT_WRITE_ACCESS = "hasRole('ADMIN') or hasAuthority('student:write:*')";
    private static final String STUDENT_MANAGE_ACCESS = "hasRole('ADMIN') or hasAuthority('student:manage:*')";

    private final StudentService studentService;

    @PreAuthorize(STUDENT_WRITE_ACCESS)
    @Operation(
        summary = "학생 등록",
        description = """
            새로운 학생을 등록합니다.

            사용 사례:
            - 신규 학생 입학 시 정보 등록
            - 연락처 및 기본 인적 사항 초기 설정

            동작 방식:
            - 이름과 전화번호 조합으로 중복 등록 여부를 확인합니다.
            - classroomId로 삭제되지 않은 분반을 확인한 뒤 학생과 연결합니다.
            - 초기 상태는 자동으로 'ENROLLED(재학)'로 설정됩니다.

            사이드 이펙트:
            - students 테이블에 새 학생 레코드가 생성됩니다.
            - 중복된 학생(이름+전화번호 동일)일 경우 DuplicateStudentException이 발생합니다.
            """
    )
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
        @Valid @RequestBody CreateStudentRequest request
    ) {
        log.debug("POST /api/v1/students - 학생 생성 요청: {}", request.name());
        StudentResponse response = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize(STUDENT_MANAGE_ACCESS)
    @Operation(
        summary = "학생 수정",
        description = """
            기존 학생의 정보를 수정합니다.

            사용 사례:
            - 학생의 연락처 변경
            - 학생 상태 변경 (졸업, 휴학, 제적 등)
            - 인적 사항 오류 정정

            동작 방식:
            - 전달된 필드만 반영됩니다 (Patch).
            - classroomId가 전달되면 소속 분반을 변경합니다.
            - 수정 후 이름과 전화번호 조합이 다른 학생과 중복되는지 검사합니다.

            사이드 이펙트:
            - students 테이블의 학생 정보가 즉시 업데이트됩니다.
            - 상태 변경은 향후 출석부 생성 및 통계 집계 범위에 영향을 줄 수 있습니다.
            """
    )
    @PatchMapping("/{studentId}")
    public ResponseEntity<StudentResponse> updateStudent(
        @Parameter(description = "학생 식별자", example = "1")
        @PathVariable Long studentId,
        @Valid @RequestBody UpdateStudentRequest request
    ) {
        log.debug("PATCH /api/v1/students/{} - 학생 수정 요청", studentId);
        StudentResponse response = studentService.updateStudent(studentId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(STUDENT_MANAGE_ACCESS)
    @Operation(
        summary = "학생 삭제",
        description = """
            학생을 삭제합니다.

            사용 사례:
            - 잘못 등록된 학생 정보 삭제 처리
            - 운영상 데이터 정리가 필요한 경우

            주의 사항:
            - 현재 구현은 soft delete를 수행합니다.
            - 삭제된 학생은 목록 조회와 상세 조회에서 제외됩니다.

            사이드 이펙트:
            - students 테이블의 isDeleted 값이 true로 변경됩니다.
            """
    )
    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> deleteStudent(
        @Parameter(description = "학생 식별자", example = "1")
        @PathVariable Long studentId
    ) {
        log.debug("DELETE /api/v1/students/{} - 학생 삭제 요청", studentId);
        studentService.deleteStudentById(studentId);
        return ResponseEntity.noContent().build();
    }
}
