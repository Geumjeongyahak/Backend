package geumjeongyahak.domain.subject.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.domain.subject.service.SubjectService;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectDetailResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Tag(
    name = "Subject",
    description = """
        과목 목록 및 상세 정보를 조회하는 API입니다.
        과목은 특정 분반의 특정 요일/교시 정기 수업 편성을 의미합니다.
        과목 등록, 수정, 삭제는 Subject Admin API를 사용하세요.
        """
)
public class SubjectController {

    private static final String SUBJECT_READ_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')";

    private final SubjectService subjectService;

    @PreAuthorize(SUBJECT_READ_ACCESS)
    @Operation(
        summary = "과목 단건 조회",
        description = """
            과목 식별자(ID)로 특정 과목의 상세 정보를 조회합니다.

            권한 정책:
            - 봉사자, 매니저, 관리자 역할만 조회할 수 있습니다.

            응답 정보:
            - 과목명, 분반 ID, 교사 ID, 운영 기간, 담당 교사 배정 기간, 요일, 교시, 시작/종료 시간, 활성화 여부를 반환합니다.
            - 교사가 아직 배정되지 않은 과목은 teacherId가 null로 반환됩니다.
            - 교사가 아직 배정되지 않은 과목은 assignedFrom과 assignedTo도 null로 반환됩니다.

            운영 정책:
            - Subject는 하루 단위 수업 기록이 아니라 정기 수업 편성 정보입니다.
            - 실제 날짜별 수업 운영 정보는 Lesson에서 관리합니다.

            사이드 이펙트:
            - 읽기 전용 API이며 과목 또는 수업 데이터를 변경하지 않습니다.
            """
    )
    @GetMapping("/{subjectId}")
    public ResponseEntity<SubjectDetailResponse> getSubject(
        @Parameter(description = "과목 식별자", example = "1")
        @PathVariable Long subjectId
    ) {
        log.debug("GET /api/v1/subjects/{} - 과목 단건 조회 요청", subjectId);
        SubjectDetailResponse response = subjectService.getSubject(subjectId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(SUBJECT_READ_ACCESS)
    @Operation(
        summary = "과목 목록 조회",
        description = """
            과목 목록을 조회합니다.

            권한 정책:
            - 봉사자, 매니저, 관리자 역할만 조회할 수 있습니다.

            검색 정책:
            - classroomId가 없으면 전체 과목을 조회합니다.
            - classroomId가 있으면 해당 분반이 존재하는지 확인한 뒤, 해당 분반의 과목만 조회합니다.

            운영 정책:
            - 활성/비활성 과목을 모두 반환합니다.
            - 교사가 아직 배정되지 않은 과목도 목록에 포함될 수 있습니다.
            - 교사가 아직 배정되지 않은 과목은 teacherId, assignedFrom, assignedTo가 null일 수 있습니다.
            - 과목은 특정 분반의 특정 요일/교시 정기 수업 편성 단위입니다.

            사이드 이펙트:
            - 읽기 전용 API이며 과목 또는 수업 데이터를 변경하지 않습니다.
            """
    )
    @GetMapping
    public ResponseEntity<List<SubjectDetailResponse>> getAllSubjects(
        @Parameter(description = "분반 식별자. 전달하면 해당 분반의 과목만 조회합니다.", example = "1")
        @RequestParam(required = false) Long classroomId
    ) {
        log.debug("GET /api/v1/subjects - 과목 목록 조회 요청 (classroomId={})", classroomId);
        return ResponseEntity.ok(subjectService.getAllSubjects(classroomId));
    }
}
