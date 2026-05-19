package geumjeongyahak.domain.channel.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.domain.channel.service.ChannelCrudService;
import geumjeongyahak.domain.channel.v1.dto.request.CreateChannelRequest;
import geumjeongyahak.domain.channel.v1.dto.request.UpdateChannelRequest;
import geumjeongyahak.domain.channel.v1.dto.response.ChannelResponse;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels")
@Tag(
        name = "Channel",
        description = """
                게시글이 소속되는 채널을 생성, 조회, 수정, 삭제하는 API입니다.
                이 컨트롤러는 운영자가 수동으로 관리하는 STANDALONE 채널만 다룹니다.
                부서/분반 같은 도메인 연동 채널 생성은 이벤트 기반 SystemChannelService가 담당합니다.
                """
)
public class ChannelAdminController {
    private final ChannelCrudService channelCrudService;

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('channel:manage:*')")
    @Operation(
            summary = "채널 생성",
            description = """
                    새로운 독립 채널을 생성합니다.

                    사용 사례:
                    - 운영자가 공지사항, 자료실, 자유게시판, 의견 수렴 게시판 같은 독립 채널 생성
                    - 기본 접근 수준만 다르게 둔 별도 게시판 생성

                    요청 시 확인할 핵심 규칙:
                    - channelType은 NOTICE, EVENT, RESOURCE, CUSTOM만 허용됩니다.
                    - channelType을 생략하면 CUSTOM으로 생성됩니다.
                    - 생성되는 채널은 항상 STANDALONE / refId=null 입니다.
                    - accessLevel이 기본 읽기/댓글/글쓰기 허용 범위를 결정합니다.

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
        log.debug("POST /api/v1/channels - 채널 생성 요청");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(channelCrudService.createChannel(request));
    }

    @PreAuthorize("hasRole('ADMIN') or hasPermission(#id, 'channel', 'manage')")
    @Operation(
            summary = "채널 수정",
            description = """
                    기존 커스텀 채널의 설정을 수정합니다.

                    사용 사례:
                    - 채널 이름이나 설명 변경
                    - 채널 숨김/재공개 처리
                    - 접근 수준(accessLevel) 변경
                    - 기본 채널 여부 조정

                    주의할 점:
                    - 전달한 필드만 반영됩니다.
                    - 시스템 채널은 이 API로 수정할 수 없습니다.
                    - 변경값이 하나도 없으면 오류가 반환됩니다.

                    사이드 이펙트:
                    - channels 테이블의 메타데이터가 즉시 변경됩니다.
                    - accessLevel, isActive 변경은 이후 게시글 작성/조회 경험에 바로 영향을 줄 수 있습니다.
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

    @PreAuthorize("hasRole('ADMIN') or hasPermission(#id, 'channel', 'manage')")
    @Operation(
            summary = "채널 삭제",
            description = """
                    커스텀 채널을 소프트 삭제합니다.

                    사용 사례:
                    - 운영 종료된 채널 정리
                    - 잘못 생성한 채널을 시스템 목록에서 완전히 제외

                    동작 방식:
                    - 물리 삭제가 아니라 isDeleted=true, isActive=false로 처리됩니다.
                    - USER_MANAGED 채널만 삭제할 수 있습니다.
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
