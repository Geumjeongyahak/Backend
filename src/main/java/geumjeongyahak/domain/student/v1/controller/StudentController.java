package geumjeongyahak.domain.student.v1.controller;

import java.util.List;

import geumjeongyahak.domain.student.service.StudentService;
import geumjeongyahak.domain.student.v1.dto.request.StudentSearchRequest;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(
    name = "Student",
    description = """
        학생 목록 및 상세 정보를 조회하는 API입니다.
        봉사자(교사)가 수업 진행 및 출석 체크를 위해 학생 정보를 조회할 때 사용합니다.
        학생 정보의 등록/수정/삭제는 Student Admin API를 사용하세요.
        """
)
public class StudentController {

    private static final String TEACHER_OR_HIGHER_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')";

    private final StudentService studentService;

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "학생 목록 조회",
        description = """
            전체 학생 목록을 배열로 조회합니다.

            사용 사례:
            - 수업 배정을 위해 전체 학생 현황 파악
            - 이름, 상태, 분반 필터를 통한 학생 검색
            - 출석부 생성을 위한 학생 목록 로딩

            동작 방식:
            - 이름(name) 검색은 부분 일치로 동작합니다.
            - 상태(status) 필터를 통해 재학/휴학/졸업생을 구분하여 조회할 수 있습니다.
            - 분반(classroomId) 필터를 통해 특정 분반 소속 학생만 조회할 수 있습니다.
            - 결과는 이름 오름차순으로 정렬됩니다.

            사이드 이펙트:
            - 읽기 전용 API이며 학생 데이터를 변경하지 않습니다.
            """
    )
    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents(
        @ParameterObject @Valid StudentSearchRequest request
    ) {
        log.debug("GET /api/v1/students - 학생 목록 조회 요청");
        List<StudentResponse> response = studentService.getAllStudents(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "학생 단건 조회",
        description = """
            학생 식별자(ID)로 특정 학생의 상세 정보를 조회합니다.

            사용 사례:
            - 특정 학생의 상세 프로필 확인
            - 수업 로그 작성 시 학생 정보 참조

            응답 정보:
            - 학생 이름, 전화번호, 상태, 비고(설명), 소속 분반 정보

            사이드 이펙트:
            - 읽기 전용 API이며 학생 데이터를 변경하지 않습니다.
            """
    )
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
