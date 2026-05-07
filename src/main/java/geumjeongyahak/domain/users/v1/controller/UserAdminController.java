package geumjeongyahak.domain.users.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.users.service.UserCrudService;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.UpdateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.UserPaginationRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserSimpleResponse;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(
    name = "User Admin",
    description = """
        사용자 생성, 조회, 수정, 삭제를 담당하는 관리자/운영자용 API입니다.
        사용자 계정 자체와 기본 역할, 소속 부서, 로그인용 이메일 정보를 함께 관리할 때 사용합니다.
        본인 전용 조회/수정은 User Self API가 담당하고, 이 API는 타인 포함 전체 사용자 운영에 집중합니다.
        """
)
public class UserAdminController {
    private final UserCrudService userCrudService;

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:read:*')")
    @Operation(
        summary = "사용자 목록 조회",
        description = """
            전체 사용자 목록을 페이지네이션하여 조회합니다.

            사용 사례:
            - 관리자 화면에서 전체 사용자 현황 조회
            - 역할별 계정 운영 대상 확인
            - 사용자 상세 화면 진입 전 목록 로딩

            동작 방식:
            - page, size 기준으로 페이지네이션합니다.
            - 응답에는 사용자 기본 정보만 포함되며 개별 권한 목록은 포함하지 않습니다.

            사이드 이펙트:
            - 읽기 전용 API이며 사용자 데이터나 인증 정보를 변경하지 않습니다.
            """
    )
    @GetMapping
    public ResponseEntity<PaginationResponse<UserSimpleResponse>> getAllUsers(
            @ParameterObject @Valid UserPaginationRequest request
    ) {
        log.debug("GET /api/v1/users - 사용자 목록 조회 요청");
        PaginationResponse<UserSimpleResponse> response = userCrudService.getAllUsersPagination(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:read:*')")
    @Operation(
        summary = "사용자 상세 조회",
        description = """
            사용자 ID로 특정 사용자의 상세 정보를 조회합니다.

            사용 사례:
            - 관리자 상세 화면에서 사용자 프로필 확인
            - 권한 부여 전 현재 역할/소속/권한 확인
            - 부서 이동 또는 역할 변경 전 원본 데이터 확인

            응답 정보:
            - 이름, 닉네임, 이메일, 전화번호
            - 기본 역할(role)
            - 소속 부서 ID
            - 사용자에게 직접 부여된 세부 권한 목록
            - 생성/수정 시각

            사이드 이펙트:
            - 읽기 전용 API이며 사용자 상태를 변경하지 않습니다.
            """
    )
    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> getUserById(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        log.debug("GET /api/v1/users/{} - 사용자 상세 조회 요청", userId);
        UserDetailResponse response = userCrudService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:write:*')")
    @Operation(
        summary = "사용자 생성",
        description = """
            새로운 사용자를 생성합니다.

            사용 사례:
            - 신규 봉사자 계정 생성
            - 운영진/관리자 계정 초기 등록
            - 특정 부서 소속 사용자 선등록

            동작 방식:
            - nickname, email 중복 여부를 먼저 검사합니다.
            - 요청 role을 기본 역할로 저장합니다.
            - departmentId가 있으면 해당 부서를 연결합니다.
            - 사용자 레코드 생성 후 Local 로그인 자격 증명(email, password)을 함께 생성합니다.

            사이드 이펙트:
            - users 테이블에 새 사용자가 저장됩니다.
            - 인증 도메인에 local credential이 함께 생성됩니다.
            - 중복 이메일/닉네임이면 생성되지 않고 예외가 반환됩니다.
            """
    )
    @PostMapping
    public ResponseEntity<UserDetailResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        log.debug("POST /api/v1/users - 사용자 생성 요청: {}", request.email());
        UserDetailResponse response = userCrudService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:manage:*')")
    @Operation(
        summary = "사용자 수정",
        description = """
            기존 사용자의 정보를 수정합니다.

            사용 사례:
            - 운영자가 사용자 이름/연락처 정정
            - 역할(role) 변경
            - 소속 부서 변경
            - 로그인 이메일 또는 비밀번호 재설정

            동작 방식:
            - 전달한 필드만 반영합니다.
            - 닉네임이나 이메일이 변경되면 중복 여부를 다시 검사합니다.
            - 비밀번호가 전달되면 Local credential의 비밀번호 해시를 갱신합니다.
            - 이메일이 변경되면 사용자 기본 이메일과 Local credential 이메일을 함께 갱신합니다.

            사이드 이펙트:
            - users 테이블의 기본 정보가 변경됩니다.
            - 이메일 또는 비밀번호 변경 시 인증 정보도 함께 변경됩니다.
            - 역할/부서 변경은 이후 인증 및 운영 화면 접근 범위에 직접 영향을 줄 수 있습니다.
            """
    )
    @PatchMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> updateUser(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        log.debug("PATCH /api/v1/users/{} - 사용자 수정 요청", userId);
        UserDetailResponse response = userCrudService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:manage:*')")
    @Operation(
        summary = "사용자 삭제",
        description = """
            사용자를 삭제합니다.

            사용 사례:
            - 잘못 생성한 계정 제거
            - 더 이상 사용하지 않는 내부 운영 계정 정리

            동작 방식:
            - 대상 사용자가 존재하는지 먼저 확인합니다.
            - 현재 구현은 사용자 레코드를 삭제하는 방식으로 동작합니다.

            사이드 이펙트:
            - 사용자 레코드가 삭제됩니다.
            - 연관 엔티티의 cascade/orphanRemoval 설정에 따라 사용자 권한, 자격 증명 등 연관 데이터에도 영향이 발생할 수 있습니다.
            - 삭제 이후 해당 계정으로의 운영 조회/수정은 불가능합니다.
            """
    )
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        log.debug("DELETE /api/v1/users/{} - 사용자 삭제 요청", userId);
        userCrudService.deleteUserById(userId);
        return ResponseEntity.noContent().build();
    }
}
