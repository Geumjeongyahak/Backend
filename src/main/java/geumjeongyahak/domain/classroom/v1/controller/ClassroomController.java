package geumjeongyahak.domain.classroom.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.domain.classroom.service.ClassroomCrudService;
import geumjeongyahak.domain.classroom.v1.dto.request.ClassroomPaginationRequest;
import geumjeongyahak.domain.classroom.v1.dto.request.CreateClassroomRequest;
import geumjeongyahak.domain.classroom.v1.dto.request.UpdateClassroomRequest;
import geumjeongyahak.domain.classroom.v1.dto.response.ClassroomDetailResponse;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/classrooms")
@Tag(name = "Classroom", description = "분반 관리 API")
public class ClassroomController {
    private final ClassroomCrudService classroomCrudService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "분반 생성", description = "새로운 분반을 생성합니다.")
    @PostMapping
    public ResponseEntity<ClassroomDetailResponse> createClassroom(
        @Valid @RequestBody CreateClassroomRequest request
    ) {
        log.debug("POST /api/v1/classrooms - 분반 생성 요청: {}", request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(
            classroomCrudService.createClassroom(request)
        );
    }

    @Operation(summary = "분반 상세 조회", description = "특정 분반의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ClassroomDetailResponse> getClassroomDetail(
        @PathVariable Long id
    ) {
        log.debug("GET /api/v1/classrooms/{} - 분반 상세 조회 요청", id);
        return ResponseEntity.ok(
            classroomCrudService.getClassroomDetail(id)
        );
    }

    @Operation(summary = "분반 목록 조회", description = "분반 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getClassrooms(
        @ParameterObject @Valid ClassroomPaginationRequest request
    ) {
        log.debug("GET /api/v1/classrooms - 분반 목록 조회 요청: name={}, type={}",
            request.getName(), request.getType());
        return ResponseEntity.ok(
            classroomCrudService.getClassrooms(request)
        );
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "분반 수정",
            description = "기존 분반 정보를 수정합니다. 전달된 필드만 변경하며 description은 빈 문자열로 비울 수 있습니다."
    )
    @PatchMapping("/{id}")
    public ResponseEntity<ClassroomDetailResponse> updateClassroom(
        @PathVariable Long id,
        @Valid @RequestBody UpdateClassroomRequest request
    ) {
        log.debug("PATCH /api/v1/classrooms/{} - 분반 수정 요청: {}", id, request.name());
        return ResponseEntity.ok(
            classroomCrudService.updateClassroom(id, request)
        );
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "분반 삭제", description = "기존 분반을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClassroom(
        @PathVariable Long id
    ) {
        log.debug("DELETE /api/v1/classrooms/{} - 분반 삭제 요청", id);
        classroomCrudService.deleteClassroom(id);
        return ResponseEntity.noContent().build();
    }
}
