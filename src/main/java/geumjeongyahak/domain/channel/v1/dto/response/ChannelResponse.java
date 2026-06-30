package geumjeongyahak.domain.channel.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.channel.entity.Channel;

import java.time.LocalDateTime;

@Schema(
        description = """
                채널 단건 조회/목록 조회/생성/수정 응답에 공통으로 사용되는 채널 응답 DTO입니다.
                채널의 표시 정보, 연동 구분, 소속 대상, 접근 수준, 활성 상태, 최근 게시 시각을 함께 제공합니다.
                관리자 화면에서는 이 응답만으로 채널의 운영 범위와 현재 상태를 파악할 수 있습니다.
                """
)
public record ChannelResponse(
        @Schema(description = "채널의 내부 식별자입니다. 게시글 생성 경로와 단건 조회 경로의 기준값으로 사용됩니다.", example = "2")
        Long id,

        @Schema(description = "사용자 화면에 표시되는 채널 이름입니다. 게시판 메뉴와 제목 영역에 그대로 노출될 수 있습니다.", example = "공지사항")
        String name,

        @Schema(description = "채널의 목적과 사용 범위를 설명하는 문구입니다. 관리자 운영 메모나 화면 설명 영역에 사용할 수 있습니다.", example = "기관 전체 운영 공지와 일정 변경 공지를 게시하는 기본 채널입니다.")
        String description,

        @Schema(description = "채널 유형입니다. NOTICE, EVENT, RESOURCE, CLASSROOM, DEPARTMENT, GUIDE, CUSTOM 중 하나입니다.", example = "NOTICE")
        String channelType,

        @Schema(description = "채널 연동 구분입니다. STANDALONE 또는 DOMAIN_LINKED 입니다.", example = "STANDALONE")
        String bindingType,

        @Schema(description = "분반/부서 등 시스템 채널이 참조하는 대상 ID입니다. 커스텀 채널은 null입니다.", example = "3", nullable = true)
        Long refId,

        @Schema(description = "채널의 기본 접근 수준입니다. CLOSED, READ_ONLY, READ_COMMENT, READ_WRITE 중 하나입니다.", example = "READ_ONLY")
        String accessLevel,

        @Schema(description = "비로그인 방문자의 읽기 허용 여부입니다. true이면 누구나 읽을 수 있습니다.", example = "false")
        boolean allowGuestRead,

        @Schema(description = "운영상 기본 채널로 취급되는지 여부입니다. 기본 게시판 묶음 구성에 활용할 수 있습니다.", example = "true")
        boolean isDefault,

        @Schema(description = "현재 채널이 활성 상태인지 여부입니다. false면 숨김 상태로 운영되며 일반 게시판 흐름에서 제외될 수 있습니다.", example = "true")
        boolean isActive,

        @Schema(
                description = """
                        채널에 마지막으로 게시글이 등록된 시각입니다.
                        아직 게시글이 한 건도 없으면 null일 수 있습니다.
                        채널 목록에서 최근 활동 게시판을 정렬하거나 표시할 때 사용할 수 있습니다.
                        """,
                example = "2026-04-10T19:30:00",
                nullable = true
        )
        LocalDateTime lastPostedAt
) {
    public static ChannelResponse from(Channel channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getName(),
                channel.getDescription(),
                channel.getChannelType().name(),
                channel.getBindingType().name(),
                channel.getRefId(),
                channel.getAccessLevel().name(),
                channel.isAllowGuestRead(),
                channel.isDefault(),
                channel.isActive(),
                channel.getLastPostedAt()
        );
    }
}
