package geumjeongyahak.domain.channel.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.channel.service.ChannelCrudService;
import geumjeongyahak.domain.channel.v1.dto.request.ChannelListRequest;
import geumjeongyahak.domain.channel.v1.dto.response.ChannelResponse;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels")
@Tag(
        name = "Channel",
        description = """
                게시글이 소속되는 채널을 조회하는 API입니다.
                공지사항, 분반 게시판, 부서 게시판, 커스텀 게시판을 공통 포맷으로 조회합니다.
                채널의 실제 읽기/쓰기 허용 범위는 accessLevel과 permission 해석 규칙을 함께 따릅니다.
                """
)
public class ChannelController {
    private final ChannelCrudService channelCrudService;

    @Operation(
            summary = "채널 목록 조회",
            description = """
                    조건에 맞는 채널 목록을 조회합니다.

                    사용 사례:
                    - 게시글 작성 화면에서 사용 가능한 채널 선택 목록 구성
                    - 관리자 화면에서 분반 채널/부서 채널 운영 현황 조회
                    - 기본 채널만 별도로 조회하거나, 비활성 채널만 점검

                    동작 방식:
                    - name은 부분 검색으로 동작합니다.
                    - classroomId를 주면 CLASSROOM 채널 중 해당 분반에 연결된 채널만 조회합니다.
                    - departmentId를 주면 DEPARTMENT 채널 중 해당 부서에 연결된 채널만 조회합니다.
                    - sort를 지정하지 않으면 createdAt 내림차순, id 내림차순으로 정렬됩니다.

                    사이드 이펙트:
                    - 읽기 전용 API이며 데이터를 변경하지 않습니다.
                    """
    )
    @GetMapping
    public ResponseEntity<List<ChannelResponse>> getChannels(
            @ParameterObject @Valid ChannelListRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/channels - 채널 목록 조회 요청");
        return ResponseEntity.ok(channelCrudService.getChannels(request, userDetails));
    }

    @PreAuthorize("@channelAccess.can('read', #id, principal)")
    @Operation(
            summary = "채널 단건 조회",
            description = """
                    채널 ID로 단건 상세 정보를 조회합니다.

                    사용 사례:
                    - 관리자 상세 화면에서 채널 설정 확인
                    - 게시글 작성 화면 진입 전 채널 메타데이터 확인
                    - 특정 채널의 accessLevel, 관리 방식, 연결 대상(ref) 확인

                    응답 정보:
                    - 채널 이름, 설명
                    - 채널 유형과 관리 방식
                    - 연결된 refId와 접근 수준
                    - 기본 채널 여부, 활성 여부, 마지막 게시 시각

                    사이드 이펙트:
                    - 읽기 전용 API이며 데이터를 변경하지 않습니다.
                    """
    )
    @GetMapping("/{id}")
    public ResponseEntity<ChannelResponse> getChannel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/channels/{} - 채널 단건 조회 요청", id);
        return ResponseEntity.ok(channelCrudService.getChannel(id, userDetails));
    }
}
