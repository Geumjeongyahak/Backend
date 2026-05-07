package geumjeongyahak.domain.channel.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidChannelAccessLevel;

@Schema(
        description = """
                채널 생성 요청 DTO입니다.
                관리자 수동 생성 대상인 커스텀 채널을 만들 때 사용합니다.
                생성되는 채널은 항상 USER_MANAGED / CUSTOM / refId=null 규칙을 따릅니다.
                접근 수준(accessLevel)을 통해 기본 읽기/댓글/글쓰기 허용 범위를 결정합니다.
                """
)
public record CreateChannelRequest(
        @Schema(
                description = "사용자에게 노출되는 채널 이름입니다. 목록/상세 화면과 게시글 작성 화면에서 그대로 표시됩니다.",
                example = "공지사항"
        )
        @NotBlank(message = "채널 이름은 필수입니다.")
        @Size(min = 2, max = 100, message = "채널 이름은 2자 이상 100자 이하로 입력해주세요.")
        String name,

        @Schema(
                description = """
                        채널의 운영 목적과 사용 대상을 설명하는 관리자용/사용자용 설명입니다.
                        예를 들어 공지 노출 범위, 작성 가능한 사람, 게시글 성격 등을 적어두면 운영에 도움이 됩니다.
                        필수값은 아니지만, 운영 화면이나 관리자 인수인계 문서 역할을 할 수 있습니다.
                        """,
                example = "기관 전체 운영 공지와 일정 변경 공지를 게시하는 기본 채널입니다."
        )
        String description,

        @Schema(
                description = """
                        시스템에서 기본 채널처럼 취급할지 여부입니다.
                        true이면 운영상 기본 채널 목록에 포함하는 용도로 사용할 수 있습니다.
                        예를 들어 첫 화면에 항상 노출할 기본 게시판을 구분할 때 사용합니다.
                        """,
                example = "true"
        )
        Boolean isDefault,

        @Schema(
                description = """
                        채널 활성 여부입니다.
                        true이면 일반 목록/게시글 작성 대상에서 사용할 수 있고, false이면 숨김 상태로 관리됩니다.
                        초안 상태로 먼저 생성해두고 나중에 공개할 때 false로 시작할 수 있습니다.
                        """,
                example = "true"
        )
        Boolean isActive,

        @Schema(
                description = """
                        채널의 기본 접근 수준입니다.
                        CLOSED, READ_ONLY, READ_COMMENT, READ_WRITE 중 하나를 사용합니다.
                        permission이 있으면 일부 제한을 우회할 수 있지만, 기본 공개 범위는 이 값으로 결정됩니다.
                        """,
                example = "READ_ONLY"
        )
        @NotNull(message = "접근 수준은 필수입니다.")
        @ValidChannelAccessLevel
        String accessLevel,

        @Schema(
                description = "비로그인 방문자의 읽기 허용 여부입니다. true이면 누구나 읽을 수 있습니다.",
                example = "false"
        )
        Boolean allowGuestRead
) {
}
