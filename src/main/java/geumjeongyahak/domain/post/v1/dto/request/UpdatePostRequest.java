package geumjeongyahak.domain.post.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidPostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(
        description = """
                게시글 수정 요청 DTO입니다.
                null이 아닌 필드만 반영되며, 하나도 보내지 않으면 변경 없음 오류가 발생합니다.
                제목/본문 수정부터 상태 전환, 고정 처리, 댓글 허용, 썸네일 변경까지 같은 요청 형태로 처리할 수 있습니다.
                """
)
public record UpdatePostRequest(
        @Schema(
                description = "변경할 게시글 제목입니다. 목록 노출 문구와 검색 기준이 함께 바뀝니다.",
                example = "4월 운영 공지 수정본"
        )
        @Size(min = 2, max = 255, message = "제목은 2자 이상 255자 이하로 입력해주세요.")
        String title,

        @Schema(
                description = "변경할 게시글 본문 HTML입니다. 상세 화면에 보이는 실제 내용이 즉시 교체됩니다.",
                example = "<p>변경된 운영 일정 안내입니다.</p>"
        )
        String contentHtml,

        @Schema(
                description = "변경할 게시글 상태입니다. 운영 정책에 따라 게시 상태 전환에 활용할 수 있습니다.",
                example = "PUBLISHED"
        )
        @ValidPostStatus
        String status,

        @Schema(
                description = "댓글 허용 여부를 변경합니다. 공지 운영 정책 변경이나 의견 수렴 종료 시 활용할 수 있습니다.",
                example = "false"
        )
        Boolean allowComment,

        @Schema(
                description = "대표 썸네일 URL을 변경합니다.",
                example = "https://cdn.example.com/posts/thumbnail.png"
        )
        String thumbnailUrl
) {
}
