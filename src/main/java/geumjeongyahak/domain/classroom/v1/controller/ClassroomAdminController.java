package geumjeongyahak.domain.classroom.v1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import geumjeongyahak.domain.classroom.service.ClassroomCrudService;
import geumjeongyahak.domain.classroom.v1.dto.request.CreateClassroomRequest;
import geumjeongyahak.domain.classroom.v1.dto.request.UpdateClassroomRequest;
import geumjeongyahak.domain.classroom.v1.dto.response.ClassroomDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/v1/classrooms")
@Tag(name = "Classroom Admin", description = "교실 관리 API - 관리자 전용")
@RequiredArgsConstructor
public class ClassroomAdminController {
    private final ClassroomCrudService classroomCrudService;

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('classroom:manage:*')")
    @Operation(summary = "교실 생성", description = "새로운 교실을 생성합니다.")
    @PostMapping
    public ResponseEntity<ClassroomDetailResponse> createClassroom(
        @Valid @RequestBody CreateClassroomRequest request
    ) {
        log.debug("POST /api/v1/classrooms - 교실 생성 요청: {}", request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(
            classroomCrudService.createClassroom(request)
        );
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('classroom:manage:*')")
    @Operation(summary = "교실 수정", description = "기존 교실 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ClassroomDetailResponse> updateClassroom(
        @PathVariable Long id,
        @Valid @RequestBody UpdateClassroomRequest request
    ) {
        log.debug("PUT /api/v1/classrooms/{} - 교실 수정 요청: {}", id, request.name());
        return ResponseEntity.ok(
            classroomCrudService.updateClassroom(id, request)
        );
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('classroom:manage:*')")
    @Operation(summary = "교실 삭제", description = "기존 교실을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClassroom(
        @PathVariable Long id
    ) {
        log.debug("DELETE /api/v1/classrooms/{} - 교실 삭제 요청", id);
        classroomCrudService.deleteClassroom(id);
        return ResponseEntity.noContent().build();
    }
}
