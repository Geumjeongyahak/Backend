package geumjeongyahak.domain.users.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import geumjeongyahak.domain.auth.v1.dto.response.PermissionResponse;
import geumjeongyahak.domain.classroom.v1.dto.response.ClassroomSummaryResponse;
import geumjeongyahak.domain.department.entity.DepartmentPermission;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentSimpleResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.support.UserBirthDateConverter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Schema(description = "사용자 상세 응답 DTO. 관리자 상세 화면과 본인 정보 조회 응답에 공통으로 사용합니다.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDetailResponse(
    @Schema(description = "사용자 식별자", example = "1")
    Long id,

    @Schema(description = "사용자 이름", example = "홍길동")
    String name,

    @Schema(description = "사용자 기본 이메일이자 Local 로그인 이메일로 사용될 수 있는 값", example = "user@example.com")
    String email,

    @Schema(description = "연락 가능한 전화번호", example = "010-1234-5678")
    String phoneNumber,

    @Schema(description = "사용자 기본 역할(role)", examples = { "ADMIN", "MANAGER", "VOLUNTEER", "GUEST" })
    String role,

    @Schema(description = "소속 부서. 소속 부서가 없거나 교원 해제 상태이면 null일 수 있습니다.", nullable = true)
    DepartmentSimpleResponse department,

    @Schema(description = "교원으로 배정된 분반. 교원 승인 전이거나 교원 해제 상태이면 null일 수 있습니다.", nullable = true)
    ClassroomSummaryResponse classroom,

    @Schema(description = "사용자 생년월일", example = "1990-01-01", nullable = true)
    LocalDate birthDate,

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "교원 활동 시작일. 교원 승인 전이면 null입니다.", example = "2026-02-01", nullable = true)
    LocalDate teacherStartAt,

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "교원 활동 종료일. role=GUEST로 교원 해제 처리된 경우 처리일로 설정됩니다.", example = "2026-06-30", nullable = true)
    LocalDate teacherEndAt,

    @Schema(description = "사용자에게 부여된 직접 권한과 부서 직책 권한 목록. 직접 권한은 MANUAL, 부서 권한은 MEMBER/MANAGER로 source가 표시됩니다.")
    List<PermissionResponse> permissions,

    @Schema(description = "사용자가 교사로 담당 중인 활성 과목 목록")
    List<TeacherAssignmentResponse> teacherAssignments,

    @Schema(description = "사용자 계정 생성 일시", example = "2024-01-01T12:00:00")
    LocalDateTime createdAt,

    @Schema(description = "사용자 기본 정보 마지막 수정 일시", example = "2024-01-02T15:30:00")
    LocalDateTime updatedAt
) {
    public static UserDetailResponse from(User user) {
        return from(user, List.of(), List.of());
    }

    public static UserDetailResponse from(User user, Collection<DepartmentPermission> departmentPermissions) {
        return from(user, departmentPermissions, List.of());
    }

    public static UserDetailResponse from(User user, List<TeacherAssignmentResponse> teacherAssignments) {
        return from(user, List.of(), teacherAssignments);
    }

    public static UserDetailResponse from(
        User user,
        Collection<DepartmentPermission> departmentPermissions,
        List<TeacherAssignmentResponse> teacherAssignments
    ) {
        return new UserDetailResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getRole().name(),
            user.getDepartment() != null ? DepartmentSimpleResponse.from(user.getDepartment()) : null,
            user.getClassroom() != null ? ClassroomSummaryResponse.from(user.getClassroom()) : null,
            UserBirthDateConverter.toBirthDate(user.getResidentRegistrationNumberPrefix()),
            user.getTeacherStartAt(),
            user.getTeacherEndAt(),
            buildPermissions(user, departmentPermissions),
            teacherAssignments,
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private static List<PermissionResponse> buildPermissions(
        User user, Collection<DepartmentPermission> departmentPermissions
    ) {
        List<PermissionResponse> permissions = new ArrayList<>();
        user.getPermissions().forEach(permission -> {
            permissions.add(PermissionResponse.from(
                permission.getId(),
                permission.toAuthorityCode(),
                PermissionResponse.SOURCE_MANUAL
            ));
        });
        departmentPermissions.forEach(permission -> {
            permissions.add(PermissionResponse.from(
                permission.getId(),
                permission.toAuthorityCode(),
                permission.getRoleType().name()
            ));
        });
        return permissions;
    }

    public Long departmentId() {
        return department != null ? department.id() : null;
    }

    public String departmentName() {
        return department != null ? department.name() : null;
    }

    public Long classroomId() {
        return classroom != null ? classroom.id() : null;
    }

    public String classroomName() {
        return classroom != null ? classroom.name() : null;
    }
}
