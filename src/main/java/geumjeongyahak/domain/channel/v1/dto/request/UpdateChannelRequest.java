package geumjeongyahak.domain.channel.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidChannelAccessLevel;

@Schema(
        description = """
                채널 수정 요청 DTO입니다.
                전달한 필드만 부분적으로 반영되며, 아무 필드도 보내지 않으면 변경 없음 오류가 발생합니다.
                ChannelCrudService는 USER_MANAGED 커스텀 채널만 수정합니다.
                숨김은 별도 API로 분리하지 않고 isActive 수정으로 함께 처리합니다.
                """
)
public record UpdateChannelRequest(
        @Schema(
                description = "변경할 채널 이름입니다. 사용자 화면, 좌측 게시판 메뉴, 관리자 목록에 즉시 반영됩니다.",
                example = "운영 공지사항"
        )
        @Size(min = 2, max = 100, message = "채널 이름은 2자 이상 100자 이하로 입력해주세요.")
        String name,

        @Schema(
                description = "채널 목적, 게시 대상, 운영 규칙 등을 설명하는 문구를 수정합니다. 인수인계나 관리자 운영 메모 용도로도 유용합니다.",
                example = "기관 전체 운영 변경 사항과 행사 공지를 게시하는 채널입니다."
        )
        String description,

        @Schema(
                description = "기본 채널 여부를 변경합니다. 운영 화면의 기본 채널 분류나 초기 노출 정책에 영향을 줄 수 있습니다.",
                example = "false"
        )
        Boolean isDefault,

        @Schema(
                description = "채널 활성 상태를 변경합니다. false로 바꾸면 채널은 숨김 상태가 되며 새 게시글 작성 대상에서도 제외될 수 있습니다.",
                example = "true"
        )
        Boolean isActive,

        @Schema(
                description = """
                        변경할 기본 접근 수준입니다.
                        CLOSED, READ_ONLY, READ_COMMENT, READ_WRITE 중 하나를 사용합니다.
                        이 값은 채널의 기본 읽기/댓글/글쓰기 허용 범위를 즉시 바꿉니다.
                        """,
                example = "READ_COMMENT"
        )
        @ValidChannelAccessLevel
        String accessLevel,

        @Schema(
                description = "비로그인 방문자의 읽기 허용 여부를 변경합니다. true이면 누구나 읽을 수 있습니다.",
                example = "false"
        )
        Boolean allowGuestRead
) {
}
