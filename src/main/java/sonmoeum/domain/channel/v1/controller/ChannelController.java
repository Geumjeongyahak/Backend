package sonmoeum.domain.channel.v1.controller;

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
import sonmoeum.domain.channel.service.ChannelCrudService;
import sonmoeum.domain.channel.v1.dto.request.ChannelListRequest;
import sonmoeum.domain.channel.v1.dto.request.CreateChannelRequest;
import sonmoeum.domain.channel.v1.dto.request.UpdateChannelRequest;
import sonmoeum.domain.channel.v1.dto.response.ChannelResponse;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels")
@Tag(
        name = "Channel",
        description = """
                게시글이 소속되는 채널을 생성, 조회, 수정, 숨김, 삭제하는 API입니다.
                공지사항, 분반 게시판, 부서 게시판 같은 게시글 진입점을 관리할 때 사용합니다.
                채널 자체는 게시글 컨테이너 역할을 하며, channelType과 writerPolicy 조합에 따라 실제 운영 범위가 달라집니다.
                """
)
public class ChannelController {
    private final ChannelCrudService channelCrudService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "채널 생성",
            description = """
                    새로운 게시글 채널을 생성합니다.

                    사용 사례:
                    - 기관 전체 공지사항 채널 생성
                    - 특정 분반 전용 게시판 생성
                    - 특정 부서 전용 게시판 생성
                    - 외부 연동 기준값을 갖는 커스텀 채널 생성

                    요청 시 확인할 핵심 규칙:
                    - channelType에 맞는 참조 ID만 전달해야 합니다.
                    - slug는 삭제되지 않은 다른 채널과 중복될 수 없습니다.
                    - writerPolicy에 따라 이후 게시글 작성 가능 사용자가 달라집니다.

                    사이드 이펙트:
                    - channels 테이블에 새 레코드가 생성됩니다.
                    - 생성 직후부터 채널 목록 조회 및 게시글 작성 대상에 포함될 수 있습니다.
                    - isActive=false로 생성하면 관리 데이터로만 존재하고 일반 운영 흐름에서는 숨김 상태가 됩니다.
                    """
    )
    @PostMapping
    public ResponseEntity<ChannelResponse> createChannel(
            @Valid @RequestBody CreateChannelRequest request
    ) {
        log.debug("POST /api/v1/channels - 채널 생성 요청: {}", request.slug());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(channelCrudService.createChannel(request));
    }

    @PreAuthorize("isAuthenticated()")
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
                    - sort를 지정하지 않으면 sortOrder 오름차순, id 오름차순으로 정렬됩니다.

                    사이드 이펙트:
                    - 읽기 전용 API이며 데이터를 변경하지 않습니다.
                    """
    )
    @GetMapping
    public ResponseEntity<List<ChannelResponse>> getChannels(
            @ParameterObject @Valid ChannelListRequest request
    ) {
        log.debug("GET /api/v1/channels - 채널 목록 조회 요청");
        return ResponseEntity.ok(channelCrudService.getChannels(request));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "채널 단건 조회",
            description = """
                    채널 ID로 단건 상세 정보를 조회합니다.

                    사용 사례:
                    - 관리자 상세 화면에서 채널 설정 확인
                    - 게시글 작성 화면 진입 전 채널 메타데이터 확인
                    - 특정 채널의 writerPolicy, 활성 상태, 연결 대상(ref) 확인

                    응답 정보:
                    - 채널 이름, 슬러그, 설명
                    - 채널 유형과 연결된 분반/부서/커스텀 참조값
                    - 게시글 작성 권한 정책
                    - 기본 채널 여부, 활성 여부, 마지막 게시 시각

                    사이드 이펙트:
                    - 읽기 전용 API이며 데이터를 변경하지 않습니다.
                    """
    )
    @GetMapping("/{id}")
    public ResponseEntity<ChannelResponse> getChannel(
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/channels/{} - 채널 단건 조회 요청", id);
        return ResponseEntity.ok(channelCrudService.getChannel(id));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "채널 수정",
            description = """
                    기존 채널의 설정을 수정합니다.

                    사용 사례:
                    - 채널 이름이나 설명 변경
                    - 분반 채널을 다른 분반에 재연결
                    - 부서 채널의 writerPolicy 변경
                    - 운영상 노출 순서와 기본 채널 여부 조정

                    주의할 점:
                    - 전달한 필드만 반영됩니다.
                    - channelType을 바꾸면 참조 ID 유효성도 함께 다시 검증됩니다.
                    - slug를 바꾸면 운영 화면이나 프론트 라우팅에서 사용하는 식별값이 달라질 수 있습니다.
                    - 변경값이 하나도 없으면 오류가 반환됩니다.

                    사이드 이펙트:
                    - channels 테이블의 메타데이터가 즉시 변경됩니다.
                    - writerPolicy, isActive, sortOrder 변경은 이후 게시글 작성/조회 경험에 바로 영향을 줄 수 있습니다.
                    """
    )
    @PutMapping("/{id}")
    public ResponseEntity<ChannelResponse> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChannelRequest request
    ) {
        log.debug("PUT /api/v1/channels/{} - 채널 수정 요청", id);
        return ResponseEntity.ok(channelCrudService.updateChannel(id, request));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "채널 숨김",
            description = """
                    채널을 비활성화하여 운영 목록에서 숨깁니다.

                    사용 사례:
                    - 더 이상 사용하지 않는 임시 채널 숨김
                    - 특정 기간 동안 게시글 작성 대상에서 제외
                    - 채널을 삭제하기 전 노출만 먼저 중단

                    사이드 이펙트:
                    - isActive 값이 false로 변경됩니다.
                    - 이후 채널 목록, 게시글 작성 대상, 일부 게시글 조회 조건에서 제외될 수 있습니다.
                    - 데이터 자체는 삭제되지 않으므로 다시 show API로 복구할 수 있습니다.
                    """
    )
    @PatchMapping("/{id}/hide")
    public ResponseEntity<ChannelResponse> hideChannel(
            @PathVariable Long id
    ) {
        log.debug("PATCH /api/v1/channels/{}/hide - 채널 숨김 요청", id);
        return ResponseEntity.ok(channelCrudService.hideChannel(id));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "채널 표시",
            description = """
                    비활성화된 채널을 다시 활성 상태로 전환합니다.

                    사용 사례:
                    - 임시 중지했던 공지 채널 재오픈
                    - 숨겨둔 분반/부서 채널 재사용

                    사이드 이펙트:
                    - isActive 값이 true로 변경됩니다.
                    - 채널이 다시 일반 조회 목록과 게시글 작성 대상에 포함될 수 있습니다.
                    - 기존 게시글 데이터는 그대로 유지된 상태에서 노출만 복구됩니다.
                    """
    )
    @PatchMapping("/{id}/show")
    public ResponseEntity<ChannelResponse> showChannel(
            @PathVariable Long id
    ) {
        log.debug("PATCH /api/v1/channels/{}/show - 채널 표시 요청", id);
        return ResponseEntity.ok(channelCrudService.showChannel(id));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "채널 삭제",
            description = """
                    채널을 소프트 삭제합니다.

                    사용 사례:
                    - 운영 종료된 채널 정리
                    - 잘못 생성한 채널을 시스템 목록에서 완전히 제외

                    동작 방식:
                    - 물리 삭제가 아니라 isDeleted=true, isActive=false로 처리됩니다.
                    - 이후 동일 ID 채널은 일반 조회에서 찾을 수 없습니다.
                    - 기존 데이터 추적이나 운영 이력 관리를 위해 DB 레코드는 남길 수 있습니다.

                    사이드 이펙트:
                    - 채널은 일반 목록, 단건 조회, 게시글 작성 대상에서 제거됩니다.
                    - 이미 연결된 게시글 정책은 별도 후속 처리 정책에 따라 검토가 필요할 수 있습니다.
                    """
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable Long id
    ) {
        log.debug("DELETE /api/v1/channels/{} - 채널 삭제 요청", id);
        channelCrudService.deleteChannel(id);
        return ResponseEntity.noContent().build();
    }
}
