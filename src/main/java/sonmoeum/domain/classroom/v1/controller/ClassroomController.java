package sonmoeum.domain.classroom.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sonmoeum.domain.classroom.service.ClassroomCrudService;
import sonmoeum.domain.classroom.v1.dto.request.CreateClassroomRequest;
import sonmoeum.domain.classroom.v1.dto.response.ClassroomResponse;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/classrooms")
@Tag(name = "Classroom", description = "교실 관리 API")
public class ClassroomController {
    private final ClassroomCrudService classroomCrudService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "교실 생성", description = "새로운 교실을 생성합니다.")
    @PostMapping
    public ResponseEntity<ClassroomResponse> createClassroom(
        @Valid @RequestBody CreateClassroomRequest request
    ) {
        log.debug("POST /api/v1/classrooms - 교실 생성 요청: {}", request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(
            classroomCrudService.createClassroom(request)
        );
    }
}
