package sonmoeum.api.v1.classrooms;

import sonmoeum.api.v1.classrooms.dto.request.CreateClassroomRequest;
import sonmoeum.api.v1.classrooms.dto.request.UpdateClassroomRequest;
import sonmoeum.api.v1.classrooms.dto.response.ClassroomResponse;
import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.ApiResponse;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.domain.classroom.service.ClassroomService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Classrooms", description = "분반 관리 API")
@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_CLASSROOMS')")
    @Operation(summary = "분반 목록 조회", description = "페이지네이션된 분반 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<BasePageResponse<ClassroomResponse>> getClassrooms(BasePageRequest pageRequest) {
        return ApiResponse.success(classroomService.getClassroomPagination(pageRequest));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_CLASSROOMS')")
    @Operation(summary = "분반 상세 조회", description = "ID로 분반을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<ClassroomResponse> getClassroom(@PathVariable Long id) {
        return ApiResponse.success(classroomService.getClassroomById(id));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_CLASSROOMS')")
    @Operation(summary = "분반 생성", description = "새로운 분반을 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClassroomResponse> createClassroom(@Valid @RequestBody CreateClassroomRequest request) {
        return ApiResponse.success(classroomService.createClassroom(request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_CLASSROOMS')")
    @Operation(summary = "분반 수정", description = "분반 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<ClassroomResponse> updateClassroom(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassroomRequest request) {
        return ApiResponse.success(classroomService.updateClassroom(id, request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_CLASSROOMS')")
    @Operation(summary = "분반 삭제", description = "분반을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteClassroom(@PathVariable Long id) {
        classroomService.deleteClassroom(id);
        return ApiResponse.success(null);
    }
}
