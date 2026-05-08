package geumjeongyahak.domain.subject.v1.controller;

import geumjeongyahak.domain.subject.service.SubjectService;
import geumjeongyahak.domain.subject.v1.dto.request.CreateSubjectRequest;
import geumjeongyahak.domain.subject.v1.dto.request.UpdateSubjectRequest;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Tag(
    name = "Subject Admin",
    description = """
        과목 등록, 수정, 삭제를 담당하는 관리자용 API입니다.
        Subject는 특정 분반의 특정 요일/교시 정기 수업 편성입니다.
        단순 조회는 Subject API가 담당합니다.
        """
)
public class SubjectAdminController {

    private static final String SUBJECT_WRITE_ACCESS = "hasRole('ADMIN') or hasAuthority('subject:write:*')";
    private static final String SUBJECT_MANAGE_ACCESS = "hasRole('ADMIN') or hasAuthority('subject:manage:*')";

    private final SubjectService subjectService;

    @PreAuthorize(SUBJECT_WRITE_ACCESS)
    @Operation(
        summary = "과목 등록",
        description = """
            새로운 과목 편성을 등록합니다.

            권한 정책:
            - 관리자 또는 subject:write:* 권한을 가진 사용자만 등록할 수 있습니다.
            - 매니저 역할만으로는 등록할 수 없으며, 별도 권한이 필요합니다.

            생성 정책:
            - classroomId는 필수이며 존재하는 분반이어야 합니다.
            - teacherId는 선택 값입니다. 교사가 아직 정해지지 않은 과목은 teacherId 없이 생성할 수 있습니다.
            - teacherId가 전달되면 해당 사용자는 봉사자 또는 매니저 역할이어야 합니다.
            - 같은 분반에서 운영 기간이 겹치고 요일과 교시가 같은 과목은 중복으로 생성할 수 없습니다.
            - startAt은 endAt보다 늦을 수 없습니다.
            - startTime은 endTime보다 빨라야 합니다.
            - times와 period는 1 이상이어야 합니다.

            Lesson 자동 생성 정책:
            - teacherId가 있으면 SubjectCreatedEvent를 발행하고 Lesson을 자동 생성합니다.
            - teacherId가 없으면 Lesson을 자동 생성하지 않습니다.
            - 자동 생성 시 운영 기간 안에서 dayOfWeek에 해당하는 날짜를 times개까지 선택합니다.
            - 동일 교사의 같은 날짜/시간대 Lesson이 이미 있으면 해당 날짜의 자동 생성은 건너뜁니다.

            사이드 이펙트:
            - subjects 테이블에 새 과목 레코드가 생성됩니다.
            - teacherId가 있는 경우 lessons 테이블에 미래 수업 레코드가 함께 생성될 수 있습니다.
            """
    )
    @PostMapping
    public ResponseEntity<SubjectDetailResponse> createSubject(
        @RequestBody @Valid CreateSubjectRequest request
    ) {
        log.debug("POST /api/v1/subjects - 과목 등록 요청");
        SubjectDetailResponse response = subjectService.createSubject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize(SUBJECT_MANAGE_ACCESS)
    @Operation(
        summary = "과목 부분 수정",
        description = """
            기존 과목 정보를 부분 수정합니다.

            권한 정책:
            - 관리자 또는 subject:manage:* 권한을 가진 사용자만 수정할 수 있습니다.
            - subject:write:* 권한은 과목 생성 전용이며 수정 권한을 포함하지 않습니다.

            수정 정책:
            - 전달된 필드만 수정합니다.
            - classroomId가 전달되면 존재하는 분반인지 확인한 뒤 과목의 분반을 변경합니다.
            - teacherId가 전달되면 존재하는 사용자이며 봉사자 또는 매니저 역할인지 확인합니다.
            - teacherId가 null로 전달된 경우에는 기존 교사를 유지합니다.
            - 같은 분반에서 운영 기간이 겹치고 요일과 교시가 같은 다른 과목이 있으면 수정할 수 없습니다.
            - startAt은 endAt보다 늦을 수 없습니다.
            - startTime은 endTime보다 빨라야 합니다.
            - times와 period는 1 이상이어야 합니다.

            현재 Lesson 반영 정책:
            - 이 API는 현재 Subject 정보만 수정합니다.
            - 이미 생성된 Lesson의 교사, 날짜, 교시, 시간은 자동으로 변경하지 않습니다.
            - Lesson에 영향을 주는 담당 교사 배정 및 일정 변경 API는 별도 정책으로 분리될 예정입니다.

            사이드 이펙트:
            - subjects 테이블의 과목 정보가 수정됩니다.
            - 현재 구현에서는 lessons 테이블을 직접 수정하지 않습니다.
            """
    )
    @PatchMapping("/{subjectId}")
    public ResponseEntity<SubjectDetailResponse> updateSubject(
        @Parameter(description = "과목 식별자", example = "1")
        @PathVariable Long subjectId,
        @Valid @RequestBody UpdateSubjectRequest request
    ) {
        log.debug("PATCH /api/v1/subjects/{} - 과목 부분 수정 요청", subjectId);
        SubjectDetailResponse response = subjectService.updateSubject(subjectId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(SUBJECT_MANAGE_ACCESS)
    @Operation(
        summary = "과목 삭제",
        description = """
            과목을 삭제합니다.

            권한 정책:
            - 관리자 또는 subject:manage:* 권한을 가진 사용자만 삭제할 수 있습니다.
            - subject:write:* 권한은 과목 생성 전용이며 삭제 권한을 포함하지 않습니다.

            삭제 정책:
            - 현재 구현은 hard delete가 아니라 soft delete를 수행합니다.
            - subjects 테이블의 isActive 값을 false로 변경합니다.
            - 이미 비활성화된 과목을 다시 삭제 요청하면 멱등하게 성공 처리합니다.

            현재 Lesson 반영 정책:
            - 과목 삭제는 현재 이미 생성된 Lesson을 삭제하지 않습니다.
            - 과거 수업 기록과 기존 Lesson 데이터는 보존됩니다.

            사이드 이펙트:
            - subjects 테이블의 isActive 값이 false로 변경됩니다.
            """
    )
    @DeleteMapping("/{subjectId}")
    public ResponseEntity<Void> deleteSubject(
        @Parameter(description = "과목 식별자", example = "1")
        @PathVariable Long subjectId
    ) {
        log.debug("DELETE /api/v1/subjects/{} - 과목 삭제(비활성화) 요청", subjectId);
        subjectService.deleteSubject(subjectId);
        return ResponseEntity.noContent().build();
    }
}
