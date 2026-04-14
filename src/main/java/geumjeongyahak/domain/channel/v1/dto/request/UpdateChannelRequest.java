package geumjeongyahak.domain.channel.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(
        description = """
                채널 수정 요청 DTO입니다.
                전달한 필드만 부분적으로 반영되며, 아무 필드도 보내지 않으면 변경 없음 오류가 발생합니다.
                channelType 또는 참조 ID를 변경하는 경우에는 생성과 동일한 타입별 제약을 따라야 합니다.
                이름/설명/정렬뿐 아니라 채널 소속 범위와 작성 정책까지 운영 중에 조정할 수 있습니다.
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
                description = """
                        변경할 채널 슬러그입니다.
                        다른 채널과 중복될 수 없으며, 운영 화면이나 프론트 라우팅 키로 사용할 경우 영향이 있을 수 있습니다.
                        기존 링크나 프론트 라우팅 규칙을 사용 중이면 변경 영향도를 먼저 확인하는 것이 좋습니다.
                        """,
                example = "operation-notice"
        )
        @Size(min = 2, max = 100, message = "채널 슬러그는 2자 이상 100자 이하로 입력해주세요.")
        String slug,

        @Schema(
                description = "채널 목적, 게시 대상, 운영 규칙 등을 설명하는 문구를 수정합니다. 인수인계나 관리자 운영 메모 용도로도 유용합니다.",
                example = "기관 전체 운영 변경 사항과 행사 공지를 게시하는 채널입니다."
        )
        String description,

        @Schema(
                description = """
                        변경할 채널 유형입니다.
                        값 변경 시 기존 refId 해석 방식도 달라지므로, 필요한 classroomId/departmentId/customRefId를 함께 보내야 합니다.
                        예를 들어 잘못 만든 ALL 채널을 DEPARTMENT 채널로 바로 교정할 수 있습니다.
                        """,
                example = "DEPARTMENT"
        )
        String channelType,

        @Schema(
                description = "CLASSROOM 채널로 변경하거나 CLASSROOM 채널의 연결 대상을 바꿀 때 사용하는 분반 ID입니다. 실제 존재하는 분반이어야 합니다.",
                example = "5"
        )
        Long classroomId,

        @Schema(
                description = "DEPARTMENT 채널로 변경하거나 DEPARTMENT 채널의 연결 대상을 바꿀 때 사용하는 부서 ID입니다. 실제 존재하는 부서여야 합니다.",
                example = "4"
        )
        Long departmentId,

        @Schema(
                description = "CUSTOM 채널의 외부 연동 기준값 또는 사용자 정의 참조값을 변경할 때 사용합니다. 외부 시스템 매핑 키 교체에도 활용할 수 있습니다.",
                example = "20001"
        )
        Long customRefId,

        @Schema(
                description = """
                        변경할 채널 작성 권한 정책입니다.
                        채널에 새 게시글을 작성할 수 있는 사용자 범위가 즉시 달라집니다.
                        운영 규칙 변경에 직결되므로 반 게시판/부서 게시판 권한 설계와 함께 검토해야 합니다.
                        """,
                example = "DEPARTMENT_MEMBER_OR_ADMIN"
        )
        String writerPolicy,

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
                description = "채널 목록에서의 노출 순서를 수정합니다. 값이 작을수록 앞쪽에 표시되며 메뉴 정렬에도 직접 영향을 줍니다.",
                example = "20"
        )
        @PositiveOrZero(message = "정렬 순서는 0 이상이어야 합니다.")
        Integer sortOrder
) {
}
