package geumjeongyahak.domain.classroom.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.domain.classroom.service.ClassroomCrudService;
import geumjeongyahak.domain.classroom.v1.dto.request.ClassroomPaginationRequest;
import geumjeongyahak.domain.classroom.v1.dto.response.ClassroomDetailResponse;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/classrooms")
@Tag(name = "Classroom", description = "교실 관리 API")
public class ClassroomController {
    private final ClassroomCrudService classroomCrudService;

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "교실 상세 조회", description = "특정 교실의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ClassroomDetailResponse> getClassroomDetail(
        @PathVariable Long id
    ) {
        log.debug("GET /api/v1/classrooms/{} - 교실 상세 조회 요청", id);
        return ResponseEntity.ok(
            classroomCrudService.getClassroomDetail(id)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "교실 목록 조회", description = "교실 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getClassrooms(
        @ParameterObject @Valid ClassroomPaginationRequest request
    ) {
        log.debug("GET /api/v1/classrooms - 교실 목록 조회 요청: name={}, type={}",
            request.getName(), request.getType());
        return ResponseEntity.ok(
            classroomCrudService.getClassrooms(request)
        );
    }
}
