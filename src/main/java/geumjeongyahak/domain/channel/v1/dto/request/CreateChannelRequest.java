package geumjeongyahak.domain.channel.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(
        description = """
                채널 생성 요청 DTO입니다.
                관리자가 공지 채널, 분반 채널, 부서 채널, 커스텀 채널을 생성할 때 사용합니다.
                channelType에 따라 함께 전달해야 하는 참조 ID가 달라집니다.
                채널은 게시글이 속하는 컨테이너이므로, 이 요청으로 게시판 구조와 작성 권한 정책이 함께 결정됩니다.

                - ALL: classroomId, departmentId, customRefId를 모두 비워야 합니다.
                - CLASSROOM: classroomId는 필수이며 나머지 참조 ID는 비워야 합니다.
                - DEPARTMENT: departmentId는 필수이며 나머지 참조 ID는 비워야 합니다.
                - CUSTOM: customRefId는 필수이며 나머지 참조 ID는 비워야 합니다.
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
                        채널의 고유 슬러그입니다.
                        프론트엔드 라우팅, 식별용 키, 운영 관리 화면에서 사람이 읽을 수 있는 식별자로 사용할 수 있습니다.
                        중복될 수 없으며 보통 영문 소문자와 하이픈 조합을 권장합니다.
                        예: 'notice', 'class-sunflower', 'dept-academic'
                        """,
                example = "notice"
        )
        @NotBlank(message = "채널 슬러그는 필수입니다.")
        @Size(min = 2, max = 100, message = "채널 슬러그는 2자 이상 100자 이하로 입력해주세요.")
        String slug,

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
                        채널의 소속 범위를 결정하는 유형입니다.
                        사용 가능한 값은 ALL, CLASSROOM, DEPARTMENT, CUSTOM 입니다.

                        - ALL: 전역 채널
                        - CLASSROOM: 특정 분반 채널
                        - DEPARTMENT: 특정 부서 채널
                        - CUSTOM: 외부 기준 ID와 연결하는 사용자 정의 채널

                        예:
                        - 공지사항 게시판: ALL
                        - 겨울반 게시판: CLASSROOM
                        - 교육연구부 게시판: DEPARTMENT
                        """,
                example = "ALL"
        )
        @NotNull(message = "채널 유형은 필수입니다.")
        String channelType,

        @Schema(
                description = """
                        CLASSROOM 채널일 때 연결할 분반 ID입니다.
                        channelType이 CLASSROOM이면 필수이고, 그 외 타입이면 비워야 합니다.
                        실제 존재하는 분반이어야 하며, 잘못된 ID를 보내면 생성이 실패합니다.
                        """,
                example = "3"
        )
        Long classroomId,

        @Schema(
                description = """
                        DEPARTMENT 채널일 때 연결할 부서 ID입니다.
                        channelType이 DEPARTMENT이면 필수이고, 그 외 타입이면 비워야 합니다.
                        실제 존재하는 부서여야 하며, 부서 게시판 권한 판단의 기준값으로도 사용됩니다.
                        """,
                example = "2"
        )
        Long departmentId,

        @Schema(
                description = """
                        CUSTOM 채널일 때 사용하는 외부 연동용 또는 운영상 사용자 정의 참조 ID입니다.
                        channelType이 CUSTOM이면 필수이고, 그 외 타입이면 비워야 합니다.
                        외부 시스템 키나 레거시 게시판 매핑값이 있을 때 활용할 수 있습니다.
                        """,
                example = "10001"
        )
        Long customRefId,

        @Schema(
                description = """
                        게시글 작성 가능 범위를 제어하는 정책입니다.
                        사용 가능한 값은 ALL_AUTHENTICATED, ADMIN_MANAGER_ONLY,
                        CLASSROOM_MANAGER_TEACHER_ONLY, DEPARTMENT_MEMBER_OR_ADMIN 입니다.

                        예:
                        - 전체 공지사항 채널: ADMIN_MANAGER_ONLY
                        - 분반 게시판: CLASSROOM_MANAGER_TEACHER_ONLY
                        - 부서 게시판: DEPARTMENT_MEMBER_OR_ADMIN

                        이 값은 게시글 생성 가능 사용자 범위를 직접 결정하므로 운영 정책과 맞춰 신중히 설정해야 합니다.
                        """,
                example = "ADMIN_MANAGER_ONLY"
        )
        String writerPolicy,

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
                        채널 목록 노출 순서를 제어하는 숫자입니다.
                        값이 작을수록 먼저 노출되며, 같은 값이면 ID 오름차순으로 정렬됩니다.
                        좌측 게시판 메뉴 정렬이나 관리자 채널 목록 기본 순서에 영향을 줍니다.
                        """,
                example = "10"
        )
        @PositiveOrZero(message = "정렬 순서는 0 이상이어야 합니다.")
        Integer sortOrder
) {
}
