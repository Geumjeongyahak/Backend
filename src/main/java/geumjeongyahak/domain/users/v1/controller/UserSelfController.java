package geumjeongyahak.domain.users.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.users.service.UserCrudService;
import geumjeongyahak.domain.users.v1.dto.request.UpdateSelfRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(
    name = "User Self",
    description = """
        현재 로그인한 사용자의 본인 조회/수정을 담당하는 API입니다.
        관리자용 User Admin API와 달리 타인 계정 운영 기능은 포함하지 않고, self access 흐름만 제공합니다.
        """
)
@PreAuthorize("isAuthenticated()")
public class UserSelfController {

    private final UserCrudService userCrudService;

    @Operation(
        summary = "사용자 본인 조회",
        description = """
            현재 로그인한 사용자의 정보를 조회합니다.

            사용 사례:
            - 마이페이지 초기 데이터 로딩
            - 프로필 수정 화면 진입 전 현재 값 조회

            응답 정보:
            - 이름, 이메일, 전화번호
            - 기본 역할(role)
            - 소속 부서 ID
            - 직접 부여된 세부 권한 목록

            사이드 이펙트:
            - 읽기 전용 API이며 사용자 상태를 변경하지 않습니다.
            """
    )
    @GetMapping
    public ResponseEntity<UserDetailResponse> getCurrentUser(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/users/me - 현재 사용자 정보 조회 요청");
        UserDetailResponse response = userCrudService.getUserById(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "사용자 본인 수정",
        description = """
            현재 로그인한 사용자의 본인 정보를 수정합니다.

            사용 사례:
            - 연락처 또는 이메일 변경
            - 비밀번호 변경

            동작 방식:
            - 전달한 필드만 반영합니다.
            - 본인 수정에서는 역할(role)과 부서(department)는 변경할 수 없습니다.
            - 이메일이 바뀌면 사용자 기본 이메일과 Local credential 이메일을 함께 갱신합니다.
            - 비밀번호가 전달되면 Local credential 비밀번호 해시를 갱신합니다.

            사이드 이펙트:
            - users 테이블의 본인 프로필 정보가 변경됩니다.
            - 인증 도메인의 로그인 이메일/비밀번호에도 영향이 갈 수 있습니다.
            """
    )
    @PatchMapping
    public ResponseEntity<UserDetailResponse> updateCurrentUser(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateSelfRequest request
    ) {
        log.debug("PATCH /api/v1/users/me - 현재 사용자 정보 수정 요청");
        UserDetailResponse response = userCrudService.updateUser(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
