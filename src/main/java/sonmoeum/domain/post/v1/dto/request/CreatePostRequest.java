package sonmoeum.domain.post.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(
        description = """
                게시글 생성 요청 DTO입니다.
                사용자가 채널 안에 새 글을 작성할 때 필요한 최소 입력만 받습니다.
                서버가 생성 시각, 작성자, 조회수 같은 내부 관리값을 자동으로 채우며, 클라이언트는 제목/본문/유형/운영 옵션만 전달하면 됩니다.
                """
)
public record CreatePostRequest(
        @Schema(
                description = """
                        게시글 제목입니다.
                        게시판 목록에서 가장 먼저 노출되는 핵심 값이며, 검색과 식별에도 직접 사용됩니다.
                        너무 짧거나 의미 없는 제목보다 공지 목적이 드러나는 문구를 권장합니다.
                        """,
                example = "4월 운영 공지"
        )
        @NotBlank(message = "제목은 필수입니다.")
        @Size(min = 2, max = 255, message = "제목은 2자 이상 255자 이하로 입력해주세요.")
        String title,

        @Schema(
                description = """
                        게시글 본문 HTML입니다.
                        에디터에서 작성한 서식, 링크, 줄바꿈 등을 포함한 최종 렌더링용 본문을 전달합니다.
                        목록 요약이 아니라 상세 화면 본문 그 자체입니다.
                        """,
                example = "<p>4월 3주차부터 수업 시작 시간이 30분 앞당겨집니다.</p>"
        )
        @NotBlank(message = "본문은 필수입니다.")
        String contentHtml,

        @Schema(
                description = """
                        게시글 유형입니다.
                        NOTICE는 공지성 글, GENERAL은 일반 글, EVENT는 행사/이벤트 성격의 글처럼 화면 분류와 강조 표시 기준으로 활용할 수 있습니다.
                        """,
                example = "NOTICE"
        )
        @NotNull(message = "게시글 유형은 필수입니다.")
        String postType,

        @Schema(
                description = """
                        게시글 상태입니다.
                        값을 생략하면 기본적으로 PUBLISHED로 처리됩니다.
                        임시저장 개념이 필요한 경우 DRAFT 같은 상태값을 활용할 수 있습니다.
                        """,
                example = "PUBLISHED"
        )
        String status,

        @Schema(
                description = """
                        상단 고정 여부입니다.
                        true이면 같은 게시판 안에서 일반 글보다 먼저 노출되도록 정렬됩니다.
                        공지, 중요한 안내, 자주 확인해야 하는 글에 주로 사용합니다.
                        """,
                example = "true"
        )
        Boolean isPinned,

        @Schema(
                description = """
                        댓글 허용 여부입니다.
                        게시판 성격에 따라 공지에는 false, 토론이나 의견 수렴 글에는 true로 둘 수 있습니다.
                        """,
                example = "false"
        )
        Boolean allowComment
) {
}
