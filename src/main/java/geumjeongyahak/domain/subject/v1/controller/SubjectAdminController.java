package geumjeongyahak.domain.subject.v1.controller;

import geumjeongyahak.domain.subject.service.SubjectService;
import geumjeongyahak.domain.subject.v1.dto.request.AssignSubjectTeacherRequest;
import geumjeongyahak.domain.subject.v1.dto.request.CreateSubjectRequest;
import geumjeongyahak.domain.subject.v1.dto.request.UpdateSubjectBasicRequest;
import geumjeongyahak.domain.subject.v1.dto.request.UpdateSubjectScheduleRequest;
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
            - teacherId가 전달되면 해당 사용자는 봉사자, 매니저 또는 관리자 역할이어야 합니다.
            - teacherId가 전달되면 teacherAssignedAt에 현재 시각을 기록합니다.
            - 같은 분반에서 운영 기간이 겹치고 요일과 교시가 같은 과목은 중복으로 생성할 수 없습니다.
            - startAt은 endAt보다 늦을 수 없습니다.
            - startTime은 endTime보다 빨라야 합니다.
            - period는 1 이상이어야 합니다.

            Lesson 자동 생성 정책:
            - teacherId가 있으면 SubjectCreatedEvent를 발행하고 Lesson을 자동 생성합니다.
            - teacherId가 없으면 Lesson을 자동 생성하지 않습니다.
            - 자동 생성 시 현재 날짜 이후 과목 운영 기간 안에서 dayOfWeek에 해당하는 날짜에 수업을 생성합니다.
            - 이미 지난 과목 운영일에 대한 과거 Lesson은 자동 생성하지 않습니다.
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
        summary = "과목 담당 교사 배정",
        description = """
            과목의 현재 담당 교사를 배정하거나 비웁니다.

            권한 정책:
            - 관리자 또는 subject:manage:* 권한을 가진 사용자만 배정할 수 있습니다.

            배정 정책:
            - teacherId가 null이면 과목의 담당 교사를 비우고 teacherAssignedAt도 null로 변경합니다.
            - teacherId가 있으면 해당 사용자는 봉사자, 매니저 또는 관리자 역할이어야 합니다.
            - teacherId가 있으면 teacherAssignedAt에 현재 시각을 기록합니다.

            Lesson 자동 반영 정책:
            - teacherId가 null이면 운영 기록이 없는 미래 SCHEDULED Lesson을 soft delete합니다.
            - teacherId가 있고 미래 Lesson이 있으면 미래 SCHEDULED Lesson 중 운영 기록이 없는 Lesson만 새 teacherId로 변경합니다.
            - teacherId가 있고 미래 Lesson이 없으면 배정일 이후 과목 운영 기간 안에서 Lesson을 자동 생성합니다.
            - 운영 기록(note, 학생 출석, 결석 요청, 진행 중인 수업 교환 요청/제안)이 있는 미래 Lesson이 있거나 새 교사의 기존 수업과 시간이 겹치면 409 Conflict를 반환합니다.
            - 이미 완료된 수업 교환 결과는 현재 Lesson의 담당 교사 상태로 존중하며, 별도의 차단 조건으로 보지 않습니다.

            사이드 이펙트:
            - subjects 테이블의 teacherId, teacherAssignedAt이 변경됩니다.
            - 조건을 만족하는 미래 lessons 테이블의 teacherId가 변경되거나, soft delete되거나, 새 Lesson이 생성될 수 있습니다.
            """
    )
    @PatchMapping("/{subjectId}/teacher")
    public ResponseEntity<SubjectDetailResponse> assignTeacher(
        @Parameter(description = "과목 식별자", example = "1")
        @PathVariable Long subjectId,
        @Valid @RequestBody AssignSubjectTeacherRequest request
    ) {
        log.debug("PATCH /api/v1/subjects/{}/teacher - 과목 담당 교사 배정 요청", subjectId);
        SubjectDetailResponse response = subjectService.assignTeacher(subjectId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(SUBJECT_MANAGE_ACCESS)
    @Operation(
        summary = "과목 일정 수정",
        description = """
            과목의 운영 기간, 요일, 교시, 수업 시간을 수정합니다.

            권한 정책:
            - 관리자 또는 subject:manage:* 권한을 가진 사용자만 수정할 수 있습니다.

            수정 정책:
            - 전달된 일정 필드만 수정합니다.
            - startAt은 endAt보다 늦을 수 없습니다.
            - startTime은 endTime보다 빨라야 합니다.
            - 같은 분반에서 운영 기간이 겹치고 요일과 교시가 같은 다른 과목이 있으면 409 Conflict를 반환합니다.

            Lesson 자동 반영 정책:
            - 과거 Lesson은 수정하거나 삭제하지 않습니다.
            - 담당 교사가 없는 과목은 Subject 일정만 수정하고 Lesson은 생성하지 않습니다.
            - period, startTime, endTime만 바뀌면 운영 기록이 없는 미래 SCHEDULED Lesson의 시간/교시를 수정합니다.
            - dayOfWeek, startAt, endAt이 바뀌면 운영 기록이 없는 미래 SCHEDULED Lesson을 soft delete한 뒤 새 일정으로 미래 Lesson을 재생성합니다.
            - 운영 기록(note, 학생 출석, 결석 요청, 진행 중인 수업 교환 요청/제안)이 있는 미래 Lesson이 있으면 409 Conflict를 반환합니다.
            - 새 일정이 담당 교사의 기존 수업과 시간이 겹치면 409 Conflict를 반환합니다.
            - 이미 완료된 수업 교환 결과는 현재 Lesson의 담당 교사 상태로 존중하며, 별도의 차단 조건으로 보지 않습니다.

            사이드 이펙트:
            - subjects 테이블의 일정 필드가 변경됩니다.
            - 조건을 만족하는 미래 lessons 테이블의 시간/교시가 변경되거나, soft delete 후 새 Lesson이 생성될 수 있습니다.
            """
    )
    @PatchMapping("/{subjectId}/schedule")
    public ResponseEntity<SubjectDetailResponse> updateSchedule(
        @Parameter(description = "과목 식별자", example = "1")
        @PathVariable Long subjectId,
        @Valid @RequestBody UpdateSubjectScheduleRequest request
    ) {
        log.debug("PATCH /api/v1/subjects/{}/schedule - 과목 일정 수정 요청", subjectId);
        SubjectDetailResponse response = subjectService.updateSchedule(subjectId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(SUBJECT_MANAGE_ACCESS)
    @Operation(
        summary = "과목 부분 수정",
        description = """
            기존 과목의 기본 정보를 부분 수정합니다.

            권한 정책:
            - 관리자 또는 subject:manage:* 권한을 가진 사용자만 수정할 수 있습니다.
            - subject:write:* 권한은 과목 생성 전용이며 수정 권한을 포함하지 않습니다.

            수정 정책:
            - 전달된 name, description 필드만 수정합니다.
            - name은 공백일 수 없습니다.
            - 교사, 담당 교사 배정 시각, 운영 기간, 요일, 교시, 시작/종료 시간은 이 API에서 수정할 수 없습니다.

            현재 Lesson 반영 정책:
            - 이 API는 Lesson에 영향을 주지 않는 Subject 기본 정보만 수정합니다.
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
        @Valid @RequestBody UpdateSubjectBasicRequest request
    ) {
        log.debug("PATCH /api/v1/subjects/{} - 과목 기본 정보 수정 요청", subjectId);
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
